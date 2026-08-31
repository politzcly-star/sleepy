package com.lingion.sleepy.ui.screen.imports

const val CQIE_ORIGIN = "https://njw.cqie.edu.cn"
const val CQIE_TIMETABLE_ENDPOINT = "/api/enrollment/timetable/student"

/**
 * M1-only schema probe. The raw response never crosses the JavaScript bridge: values are
 * projected to types/placeholders first, while small schedule numbers and strict week/time
 * expressions are retained so the parser contract can be frozen without retaining identity.
 */
const val CQIE_SCHEMA_PROBE_JS = """(function(){
  var ORIGIN = 'https://njw.cqie.edu.cn';
  var ENDPOINT = '/api/enrollment/timetable/student';
  function send(value){
    window.__cqieBridge.onSchemaProjection(JSON.stringify(value));
  }
  function findAccessToken(){
    var stored = localStorage.getItem('cqu_edu_ACCESS_TOKEN')
      || localStorage.getItem('cqu_edu_CURRENT_TOKEN') || '';
    try {
      var decoded = JSON.parse(stored);
      if (typeof decoded !== 'string') return '';
      return decoded.trim().replace(/^Bearer\s+/i, '');
    } catch (ignored) {
      return '';
    }
  }
  function sensitiveKey(key){
    return /(authorization|bearer|token|cookie|password|passwd|secret|student|studentno|studentid|user|account|phone|mobile|email|idcard|sfzh|xuehao|gonghao|(^|_)xh($|_)|(^|_)xgh($|_)|(^|_)id($|_))/i.test(key);
  }
  function scheduleString(key, value){
    var text = String(value).trim();
    if (!text || text.length > 80) return null;
    if (/^(?=.*(?:周|星期|节|:))[0-9\s,，、;；.()（）\[\]\-—~～:第周单双星期一二三四五六日天节至]+$/.test(text)) return text;
    if (/(week|weeks|weeklist|zc|zcd|skzc|qsz|jsz)/i.test(key)
        && /^[0-9\s,，、;；()（）\[\]\-—~～单双]+$/.test(text)) return text;
    return null;
  }
  function project(value, key, depth){
    if (depth > 10) return '<max-depth>';
    if (value === null) return null;
    if (Array.isArray(value)) {
      var samples = [];
      for (var i = 0; i < value.length && i < 3; i++) samples.push(project(value[i], key, depth + 1));
      return {__type:'array', __count:value.length, __samples:samples};
    }
    if (typeof value === 'object') {
      var out = {};
      Object.keys(value).sort().forEach(function(k){ out[k] = project(value[k], k, depth + 1); });
      return out;
    }
    if (typeof value === 'string') {
      if (sensitiveKey(key)) return '<redacted-string>';
      var schedule = scheduleString(key, value);
      return schedule === null ? '<string>' : schedule;
    }
    if (typeof value === 'number') {
      if (sensitiveKey(key)) return '<redacted-number>';
      return Number.isInteger(value) && value >= -1 && value <= 100 ? value : '<number>';
    }
    if (typeof value === 'boolean') return value;
    return '<' + typeof value + '>';
  }
  try {
    if (location.origin !== ORIGIN) {
      send({ok:false, kind:'WRONG_ORIGIN', origin:location.origin});
      return;
    }
    var accessToken = findAccessToken();
    var headers = {'Accept':'application/json'};
    if (accessToken) headers['Authorization'] = 'Bearer ' + accessToken;
    fetch(ENDPOINT, {
      method:'GET',
      credentials:'include',
      redirect:'manual',
      headers:headers
    }).then(function(response){
      var meta = {
        ok:false,
        status:response.status,
        redirected:response.redirected,
        contentType:response.headers.get('content-type') || ''
      };
      return response.text().then(function(text){ return {response:response, meta:meta, text:text}; });
    }).then(function(result){
      var response = result.response, meta = result.meta, text = result.text;
      if (response.status === 401 || response.status === 403) {
        meta.kind = 'SESSION_EXPIRED'; send(meta); return;
      }
      if (!response.ok || response.redirected || response.type === 'opaqueredirect') {
        meta.kind = 'HTTP_OR_REDIRECT'; send(meta); return;
      }
      if (!text || !text.trim()) {
        meta.kind = 'EMPTY'; send(meta); return;
      }
      var parsed;
      try { parsed = JSON.parse(text); }
      catch (error) { meta.kind = 'MALFORMED_OR_LOGIN_HTML'; send(meta); return; }
      meta.ok = true;
      meta.kind = 'SCHEMA';
      meta.projection = project(parsed, '', 0);
      send(meta);
    }).catch(function(error){
      send({ok:false, kind:'NETWORK', errorType:error && error.name ? error.name : 'Error'});
    });
  } catch (error) {
    send({ok:false, kind:'PROBE_ERROR', errorType:error && error.name ? error.name : 'Error'});
  }
})();"""
