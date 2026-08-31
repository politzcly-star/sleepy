[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [ValidateSet("Initialize", "Activate", "Checkpoint", "Show", "Validate", "Close")] [string]$Action,
    [string]$GoalId, [string]$ContractPath, [string]$CapsulePath, [string]$Milestone, [string]$NextAction, [string]$ReportPath, [string]$StateRoot
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $StateRoot) { $StateRoot = Join-Path $root ".codex/harness-state" }
$StateRoot = [IO.Path]::GetFullPath($StateRoot)
$activePath = Join-Path $StateRoot "active-goal.json"
$maxCapsuleBytes = 16KB

function Fail([string]$Message) { Write-Output "[FAIL] $Message"; exit 1 }
function Get-Sha256([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Resolve-InputPath([string]$Path) { if ([IO.Path]::IsPathRooted($Path)) { [IO.Path]::GetFullPath($Path) } else { [IO.Path]::GetFullPath((Join-Path $root $Path)) } }
function Get-RelativeSafePath([string]$Path) {
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw "path is outside project root" }
    ($full.Substring($root.Length).TrimStart([char[]]@('\','/')) -replace '\\','/')
}
function Get-ActiveState {
    if (-not (Test-Path -LiteralPath $activePath -PathType Leaf)) { return $null }
    try { Get-Content -Raw -LiteralPath $activePath | ConvertFrom-Json -ErrorAction Stop } catch { Fail "active Goal state is malformed" }
}
function Test-SecretLike([string]$Text) { $Text -match '(?i)(-----BEGIN (RSA|OPENSSH|PRIVATE) KEY-----|\b(api[_-]?key|secret|token|password|database_url|connectionstring)\s*[:=]|authorization\s*:\s*bearer)' }
function Get-AllowedPaths([string]$Contract) {
    $match = [regex]::Match($Contract, '(?s)<!-- harness:allowed-paths:start -->\s*(.*?)\s*<!-- harness:allowed-paths:end -->')
    if (-not $match.Success) { return @() }
    @($match.Groups[1].Value -split "`r?`n" | ForEach-Object { $_.Trim() -replace '^-\s*','' } | Where-Object { $_ })
}
function Test-CompletionReport([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { return $false }
    $full = Resolve-InputPath $Path
    if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { return $false }
    $text = Get-Content -Raw -LiteralPath $full
    foreach ($term in @('Task ID', 'Status', 'Changed Files', 'Checks Run', 'Scope Guard', 'Residual Risk', 'Next Recommended Action')) {
        if ($text -notmatch [regex]::Escape($term)) { return $false }
    }
    return $text -notmatch 'TODO'
}
function Test-State([object]$State) {
    if ($null -eq $State) { Fail "no active Goal state" }
    foreach ($name in @('GoalId','Status','ContractPath','ContractHash','ContextEpoch','CapsuleHash')) {
        if ($null -eq $State.PSObject.Properties[$name] -or [string]::IsNullOrWhiteSpace([string]$State.$name)) { Fail "active state is missing $name" }
    }
    $contract = Join-Path $root ([string]$State.ContractPath)
    if (-not (Test-Path -LiteralPath $contract -PathType Leaf)) { Fail "frozen contract is missing" }
    if ((Get-Sha256 $contract) -ne [string]$State.ContractHash) { Fail "frozen contract hash mismatch" }
    $capsule = Join-Path $StateRoot "capsule-$($State.GoalId).md"
    if (-not (Test-Path -LiteralPath $capsule -PathType Leaf)) { Fail "active capsule is missing" }
    if ((Get-Item -LiteralPath $capsule).Length -gt $maxCapsuleBytes) { Fail "capsule exceeds 16 KiB bound" }
    $text = Get-Content -Raw -LiteralPath $capsule
    if (Test-SecretLike $text) { Fail "capsule contains secret-like content" }
    $epochMatch = [regex]::Match($text, '(?im)^Context epoch:\s*(\d+)\s*$')
    if (-not $epochMatch.Success) { Fail "capsule is malformed: Context epoch is missing" }
    if ([int]$epochMatch.Groups[1].Value -ne [int]$State.ContextEpoch) { Fail "capsule context epoch is stale" }
    if ((Get-Sha256 $capsule) -ne [string]$State.CapsuleHash) { Fail "capsule hash mismatch or stale state" }
    @{ Contract = $contract; Capsule = $capsule; AllowedPaths = @(Get-AllowedPaths (Get-Content -Raw -LiteralPath $contract)) }
}

if ($Action -eq 'Initialize') {
    if ([string]::IsNullOrWhiteSpace($GoalId) -or [string]::IsNullOrWhiteSpace($ContractPath)) { Fail "Initialize requires GoalId and ContractPath" }
    if (Test-Path -LiteralPath $activePath -PathType Leaf) { Fail "an existing Goal state must be closed before Initialize can create another contract" }
    $contract = Resolve-InputPath $ContractPath
    if (-not (Test-Path -LiteralPath $contract -PathType Leaf)) { Fail "contract does not exist" }
    if (Test-SecretLike (Get-Content -Raw -LiteralPath $contract)) { Fail "contract contains secret-like content" }
    New-Item -ItemType Directory -Force -Path $StateRoot | Out-Null
    $capsule = Join-Path $StateRoot "capsule-$GoalId.md"
    if ($CapsulePath) { Copy-Item -LiteralPath $CapsulePath -Destination $capsule -Force } else { Set-Content -LiteralPath $capsule -Value "Goal ID: $GoalId`nContext epoch: 0`nContract reference / hash: $(Get-RelativeSafePath $contract) / $(Get-Sha256 $contract)`nNext action: activate after Human GO" -Encoding utf8 -NoNewline }
    if ((Get-Item -LiteralPath $capsule).Length -gt $maxCapsuleBytes -or (Test-SecretLike (Get-Content -Raw -LiteralPath $capsule))) { Fail "initial capsule is unsafe" }
    $state = [ordered]@{ GoalId=$GoalId; Status='PENDING_HUMAN_GO'; ContractPath=(Get-RelativeSafePath $contract); ContractHash=(Get-Sha256 $contract); ContextEpoch=0; CapsuleHash=(Get-Sha256 $capsule); Milestone=''; NextAction='activate after Human GO'; UpdatedUtc=(Get-Date).ToUniversalTime().ToString('o') }
    $state | ConvertTo-Json | Set-Content -LiteralPath $activePath -Encoding utf8 -NoNewline
    Write-Output "[OK] initialized pending Goal $GoalId"; exit 0
}

$state = Get-ActiveState
if ($GoalId -and $state.GoalId -ne $GoalId) { Fail "active GoalId does not match" }
if ($Action -eq 'Activate') {
    if ($state.Status -ne 'PENDING_HUMAN_GO') { Fail "Goal is not pending Human GO" }; $null = Test-State $state
    $state.Status = 'ACTIVE'; $state.NextAction = if ($NextAction) { $NextAction } else { 'implement frozen contract' }; $state.UpdatedUtc = (Get-Date).ToUniversalTime().ToString('o')
    $state | ConvertTo-Json | Set-Content -LiteralPath $activePath -Encoding utf8 -NoNewline; Write-Output "[OK] activated Goal $($state.GoalId)"; exit 0
}
if ($Action -eq 'Checkpoint') {
    $valid = Test-State $state; if ($state.Status -ne 'ACTIVE') { Fail "only an active Goal can checkpoint" }; if (-not $CapsulePath) { Fail "Checkpoint requires CapsulePath" }
    $source = Resolve-InputPath $CapsulePath; if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { Fail "capsule input does not exist" }
    if ((Get-Item -LiteralPath $source).Length -gt $maxCapsuleBytes -or (Test-SecretLike (Get-Content -Raw -LiteralPath $source))) { Fail "capsule is unsafe" }
    Copy-Item -LiteralPath $source -Destination $valid.Capsule -Force; $state.ContextEpoch = [int]$state.ContextEpoch + 1; $state.CapsuleHash = Get-Sha256 $valid.Capsule
    if ($Milestone) { $state.Milestone = $Milestone }; if ($NextAction) { $state.NextAction = $NextAction }; $state.UpdatedUtc = (Get-Date).ToUniversalTime().ToString('o')
    $state | ConvertTo-Json | Set-Content -LiteralPath $activePath -Encoding utf8 -NoNewline; Write-Output "[OK] checkpointed Goal $($state.GoalId) at epoch $($state.ContextEpoch)"; exit 0
}
if ($Action -eq 'Show' -or $Action -eq 'Validate') {
    $valid = Test-State $state
    if ($Action -eq 'Show') { [ordered]@{ GoalId=$state.GoalId; Status=$state.Status; ContextEpoch=$state.ContextEpoch; ContractPath=$state.ContractPath; ContractHash=$state.ContractHash; CapsulePath=(Get-RelativeSafePath $valid.Capsule); CapsuleHash=$state.CapsuleHash; Milestone=$state.Milestone; NextAction=$state.NextAction; AllowedPaths=$valid.AllowedPaths } | ConvertTo-Json -Compress } else { Write-Output "[OK] active Goal state is valid" }
    exit 0
}
if ($Action -eq 'Close') {
    if (-not (Test-CompletionReport $ReportPath)) { Fail "Close requires a concrete completion ReportPath that passes report validation" }
    if (Test-Path -LiteralPath $activePath) { Remove-Item -LiteralPath $activePath -Force }; $capsule = Join-Path $StateRoot "capsule-$($state.GoalId).md"; if (Test-Path -LiteralPath $capsule) { Remove-Item -LiteralPath $capsule -Force }
    Write-Output "[OK] closed Goal $($state.GoalId)"; exit 0
}
