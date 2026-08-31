package com.lingion.sleepy.ui.screen.imports

const val CQIE_ORIGIN = "https://njw.cqie.edu.cn"
const val CQIE_TIMETABLE_ENDPOINT = "/api/enrollment/timetable/student"

/**
 * Production CQIE fetch. The access token is decoded and used entirely inside the same-origin
 * WebView. Native code receives only a successful response body or a fixed, body-free error.
 */
const val CQIE_FETCH_JS = """(function(){
  var ORIGIN = 'https://njw.cqie.edu.cn';
  var ENDPOINT = '/api/enrollment/timetable/student';
  function sendError(kind, status){
    window.__cqieBridge.onResult(JSON.stringify({ok:false,kind:kind,status:status || 0}));
  }
  function findAccessToken(){
    var stored = localStorage.getItem('cqu_edu_ACCESS_TOKEN') || '';
    try {
      var decoded = JSON.parse(stored);
      if (typeof decoded !== 'string') return '';
      return decoded.trim().replace(/^Bearer\s+/i, '');
    } catch (ignored) {
      return '';
    }
  }
  try {
    if (location.origin !== ORIGIN) {
      sendError('WRONG_ORIGIN', 0);
      return;
    }
    var accessToken = findAccessToken();
    if (!accessToken) {
      sendError('SESSION_EXPIRED', 0);
      return;
    }
    fetch(ENDPOINT, {
      method:'GET',
      credentials:'include',
      redirect:'manual',
      headers:{'Accept':'application/json','Authorization':'Bearer ' + accessToken}
    }).then(function(response){
      if (response.status === 401 || response.status === 403) {
        sendError('SESSION_EXPIRED', response.status);
        return null;
      }
      if (response.redirected || response.type === 'opaqueredirect') {
        sendError('LOGIN_REDIRECT', response.status);
        return null;
      }
      if (!response.ok) {
        sendError('HTTP_ERROR', response.status);
        return null;
      }
      return response.text().then(function(text){
        if (!text || !text.trim()) {
          sendError('EMPTY', response.status);
          return;
        }
        var contentType = (response.headers.get('content-type') || '').toLowerCase();
        if (contentType.indexOf('text/html') >= 0 || /^\s*</.test(text)) {
          sendError('LOGIN_PAGE', response.status);
          return;
        }
        try { JSON.parse(text); }
        catch (ignored) {
          sendError('MALFORMED_JSON', response.status);
          return;
        }
        window.__cqieBridge.onResult(JSON.stringify({ok:true,kind:'SUCCESS',data:text}));
      });
    }).catch(function(){
      sendError('NETWORK', 0);
    });
  } catch (ignored) {
    sendError('BRIDGE_ERROR', 0);
  }
})();"""
