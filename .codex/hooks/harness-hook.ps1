param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("SessionStart", "PreToolUse", "PreCompact", "PostCompact")]
    [string]$Event,
    [Parameter(ValueFromPipeline = $true)] [string]$HookInput
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$raw = if ($HookInput) { $HookInput } else { [Console]::In.ReadToEnd() }
$payload = $null; if ($raw.Trim()) { try { $payload = $raw | ConvertFrom-Json -ErrorAction Stop } catch { $payload = $null } }

function Write-HookJson([hashtable]$Object) { $Object | ConvertTo-Json -Depth 8 -Compress }
function Get-PropertyValue($Object, [string[]]$Names) { if ($null -eq $Object) { return $null }; foreach ($name in $Names) { $p = $Object.PSObject.Properties[$name]; if ($null -ne $p -and $null -ne $p.Value) { return $p.Value } }; $null }
function Deny([string]$Reason) { Write-HookJson @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; permissionDecision = "deny"; permissionDecisionReason = "[block] $Reason" } } }
function Add-Context([string]$Message) { Write-HookJson @{ hookSpecificOutput = @{ hookEventName = "SessionStart"; additionalContext = "[info] $Message" } } }
function Test-SecretLike([string]$Text) { $Text -match '(?i)(-----BEGIN (RSA|OPENSSH|PRIVATE) KEY-----|\b(api[_-]?key|secret|token|password|database_url|connectionstring)\s*[:=]|authorization\s*:\s*bearer)' }
function Get-ActiveGoal {
    $statePath = Join-Path $root ".codex/harness-state/active-goal.json"
    if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) { return $null }
    try {
        $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json -ErrorAction Stop
        if ($state.Status -ne 'ACTIVE' -or -not $state.ContractPath -or -not $state.ContractHash -or -not $state.CapsuleHash) { return $null }
        $contract = Join-Path $root $state.ContractPath; $capsule = Join-Path (Split-Path $statePath -Parent) "capsule-$($state.GoalId).md"
        if (-not (Test-Path -LiteralPath $contract -PathType Leaf) -or -not (Test-Path -LiteralPath $capsule -PathType Leaf)) { return $null }
        if ((Get-FileHash -LiteralPath $contract -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$state.ContractHash) { return $null }
        if ((Get-Item -LiteralPath $capsule).Length -gt 16KB) { return $null }
        $capsuleText = Get-Content -Raw -LiteralPath $capsule
        if ((Test-SecretLike $capsuleText) -or (Get-FileHash -LiteralPath $capsule -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$state.CapsuleHash) { return $null }
        $contractText = Get-Content -Raw -LiteralPath $contract
        $match = [regex]::Match($contractText, '(?s)<!-- harness:allowed-paths:start -->\s*(.*?)\s*<!-- harness:allowed-paths:end -->')
        $allowed = if ($match.Success) { @($match.Groups[1].Value -split "`r?`n" | ForEach-Object { ($_.Trim() -replace '^-\s*','') } | Where-Object { $_ }) } else { @() }
        return @{ State=$state; Contract=$contract; Allowed=$allowed }
    } catch { return $null }
}
function Test-AllowedPath([string]$Path, [string[]]$Allowed) {
    if (-not $Path -or -not $Allowed -or $Allowed.Count -eq 0) { return $false }
    $p = ($Path -replace '\\','/').Trim().TrimStart('./')
    foreach ($entry in $Allowed) { $a = ($entry -replace '\\','/').Trim().TrimStart('./'); if ($p -eq $a -or ($a.EndsWith('/') -and $p.StartsWith($a))) { return $true } }
    $false
}
function Get-PatchTargets([string]$Patch) { @([regex]::Matches($Patch, '(?im)^\*\*\* (?:Update|Add|Delete) File: (.+?)\s*$') | ForEach-Object { $_.Groups[1].Value.Trim() }) }
function Test-CompletionReportArgument([string]$Command) {
    $match = [regex]::Match($Command, '(?i)-reportpath\s+(?:"([^"]+)"|''([^'']+)''|([^\s]+))')
    if (-not $match.Success) { return $false }
    $path = @($match.Groups[1].Value, $match.Groups[2].Value, $match.Groups[3].Value | Where-Object { $_ })[0]
    try {
        $full = if ([IO.Path]::IsPathRooted($path)) { [IO.Path]::GetFullPath($path) } else { [IO.Path]::GetFullPath((Join-Path $root $path)) }
        if (-not $full.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $full -PathType Leaf)) { return $false }
        $text = Get-Content -Raw -LiteralPath $full
        foreach ($term in @('Task ID','Status','Changed Files','Checks Run','Scope Guard','Residual Risk','Next Recommended Action')) { if ($text -notmatch [regex]::Escape($term)) { return $false } }
        return $text -notmatch 'TODO'
    } catch { return $false }
}

if ($Event -eq 'PreCompact') { Write-HookJson @{}; exit 0 }
if ($Event -eq 'PostCompact') { Write-HookJson @{}; exit 0 }
if ($Event -eq 'SessionStart') {
    $source = [string](Get-PropertyValue $payload @('source','session_source'))
    $goal = Get-ActiveGoal
    if ($source -match '(?i)compact|resume' -and $goal) { Add-Context "active Goal $($goal.State.GoalId), epoch $($goal.State.ContextEpoch): read frozen contract $($goal.State.ContractPath) and bounded capsule pointer before work; do not reload transcripts." }
    elseif ($source -match '(?i)compact|resume') { Add-Context "no safe active Goal capsule was injected; reload repository authority files only." }
    else { Add-Context "v5.6 harness: Human approves one Goal contract, then the Terra root proceeds through implementation, verification, focused repair, and required Luna review without phase-by-phase confirmation." }
    exit 0
}

$toolName = [string](Get-PropertyValue $payload @('tool_name','toolName','name')); $toolName = $toolName.ToLowerInvariant()
$input = Get-PropertyValue $payload @('tool_input','toolInput','input','arguments')
$command = if ($input -is [string]) { $input } else { [string](Get-PropertyValue $input @('command','cmd','script','text')) }
$isPatch = $toolName -match 'apply[_-]?patch'; $isCommand = $toolName -match 'shell|command|terminal|powershell|exec|mcp'
$goal = Get-ActiveGoal
if ($isPatch) {
    $patchText = if ($input -is [string]) { $input } else { [string](Get-PropertyValue $input @('patch','text')) }
    $targets = @(Get-PatchTargets $patchText)
    if ($patchText -match '(?im)^\*\*\* (?:Update|Add|Delete) File: .*?(?:\.env(?:\.[^\s]+)?|id_rsa|id_ed25519|\.pem|\.key)\s*$') { Deny 'patch targets a secret-like file. Record only a redacted presence summary.'; exit 0 }
    if ($goal) {
        foreach ($target in $targets) {
            if ($target -eq $goal.State.ContractPath -or $target -match '^(\.codex/(harness-state|hooks|agents|config)|scripts/goal-state\.ps1|AGENTS\.md|docs/(capability-policy|verification-and-guardrails)\.md)') { Deny 'active Goal contract or Harness authorization-policy self-modification is denied.'; exit 0 }
            if (-not (Test-AllowedPath $target $goal.Allowed)) { Deny 'patch target is outside the frozen Goal allowed scope.'; exit 0 }
        }
        if ($targets.Count -eq 0) { Deny 'ambiguous patch has no parseable target under an active Goal.'; exit 0 }
    }
    Write-HookJson @{}; exit 0
}
if (-not $isCommand) { Write-HookJson @{}; exit 0 }
$lower = $command.ToLowerInvariant()
if ($lower -match '(begin\s+(rsa|openssh|private)\s+key|private\s+key-----)') { Deny 'tool call appears to expose private-key material.'; exit 0 }
if ($lower -match '\b(get-content|type|cat|gc|select-string|sls|rg|grep|open|notepad|code)\b[^\r\n]{0,200}(\.env($|[^a-z0-9])|id_rsa|id_ed25519|\.pem|\.key)') { Deny 'tool call appears to read a secret-like file.'; exit 0 }
if (Test-SecretLike $command) { Deny 'command appears to embed credentials.'; exit 0 }
if ($lower -match '(rm\s+-rf|remove-item[^\r\n]*(recurse)[^\r\n]*(force)|git\s+reset\s+--hard|git\s+checkout\s+--\s|git\s+clean\s+-[a-z]*f|git\s+branch\s+(-d|--delete)|git\s+worktree\s+remove)') { Deny 'destructive filesystem or git command needs fresh Human authority.'; exit 0 }
if ($lower -match '\b(drop|truncate)\b|\bdelete\s+from\b|\bupdate\s+[\w.\[\]`"]+\s+set\b|\binsert\s+into\b|\b(prisma|sequelize)\s+.*migrate\b') { Deny 'database mutation or migration requires fresh approval.'; exit 0 }
$remoteMutation = 'sudo\s+su|\brm\s+-|\bmv\s+|\bcp\s+|\bchmod\s+|\bchown\s+|\btee\s+|\b(touch|mkdir|rmdir|ln|install|dd|truncate)\b|\b(sed|perl)\s+-[a-z]*i\b|>{1,2}|systemctl\s+(restart|reload|start|stop|enable|disable)|service\s+\w+\s+(restart|reload|start|stop)|docker\s+compose\s+(up|down)|kubectl\s+(apply|delete|rollout|scale)|helm\s+(upgrade|install|delete)|npm\s+run\s+deploy|pnpm\s+deploy|\bvercel\s+--prod|\bflyctl\s+deploy|\brailway\s+up'
if ($lower -match '\b(ssh|scp|rsync)\b') { if ($lower -match 'harness:server-inspection' -and $lower -notmatch $remoteMutation) { Write-HookJson @{}; exit 0 }; Deny 'remote command needs a read-only marker or fresh approved mutation route.'; exit 0 }
if ($lower -match $remoteMutation) { Deny 'deployment, restart, or production-adjacent mutation requires fresh approval.'; exit 0 }
if ($goal -and $lower -match 'scripts[\\/]goal-state\.ps1' -and $lower -match '(?i)-action\s+(initialize|activate)\b') { Deny 'active Goal authorization contract replacement is denied.'; exit 0 }
if ($goal -and $lower -match 'scripts[\\/]goal-state\.ps1' -and $lower -match '(?i)-action\s+close\b') {
    if ($lower -match 'harness:goal-close' -and (Test-CompletionReportArgument $command)) { Write-HookJson @{}; exit 0 }
    Deny 'Close requires harness:goal-close and a concrete passing completion ReportPath.'; exit 0
}
if ($goal -and $lower -match '\.codex[\\/]harness-state[\\/]' -and $lower -match '(set-content|add-content|out-file|copy-item|move-item|remove-item|>\s*[^\s]+)') { Deny 'active Goal authorization-state mutation is denied.'; exit 0 }
if ($goal -and $lower -match '(set-content|add-content|out-file|copy-item|move-item|>\s*[^\s]+)') { Deny 'shell mutation is ambiguous under an active Goal; use an exact allowed apply_patch target.'; exit 0 }
Write-HookJson @{}
