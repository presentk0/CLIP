(function() {
  // 브라우저가 만든 함수(받는 주소, 요청 방식이 담김) 백업
  const originalOpen = XMLHttpRequest.prototype.open;
  // 브라우저가 만든 함수(실제 요청 전송) 백업
  const originalSend = XMLHttpRequest.prototype.send;

  // open 함수에 익명 함수를 덮어쓰기 (URL 저장용)
  // method = 'GET'
  // url = 'https://youtube.com/api/timedtext'
  // open()을 호출한 객체의 method(순서 때문에 가져오기)와 url을 가져오기
  XMLHttpRequest.prototype.open = function(method, url) {
    // this에는 url이 없으니 open()을 호출한 url을 저장
    this.clipzyUrl = url;
    // 백업해둔 원본 함수를 수동으로 같은 상황(this)에서 같은 값(arguments)으로 실행하는 조건을 설정하기
    // apply 첫 번째 인자(누가(유튜브가 자막 요청할 때 만드는 객체) 이 함수를 실행하는지)
    // apply 두 번째 인자(arguments = 모든 인자 (method, url, ...나머지 인자)들로 실행)
    return originalOpen.apply(this, arguments);
  };

  // send 함수에 익명 함수를 덮어쓰기 (대신 응답하기)
  // send()을 호출한 객체에 익명 함수를 적용 (요청은 open 이후에 send를 하기에 같은 this를 가짐)
  XMLHttpRequest.prototype.send = function() {
    const url = this.clipzyUrl;

    // 호출한 객체의 서버 응답이 완료되면 익명 함수를 실행
    this.addEventListener('load', function() {
      // 호출한 객체의 URL이 존재하는지 && 호출한 객체의 URL에 'timedtext' 포함되어있는지 확인 && 호출한 객체의 URL에 'lang=en'이 있는지 확인 즉 영어인지 확인
      // 유튜브 자막 URL에는 'timedtext'와 'lang'이 포함됨
      if (url && url.includes('timedtext') && url.includes('lang=en')) {
        // 같은 페이지 안에서 서로 다른 환경 간 데이터 주고받기(유튜브 환경 -> 크롬 확장프로그램 환경으로 데이터 전달)
        window.postMessage({
          type: 'CLIPZY_SUBTITLE_DATA',
          // 보낼 데이터(events 안에 tStartMs, dDurationMs, segs가 있는 객체들이 담겨있음)
          response: this.responseText
          // 받을 수 있는 출처 유튜브로 제한
        }, 'https://www.youtube.com');
      }
    });

    // 백업해둔 원본 함수를 수동으로 같은 상황(this)에서 같은 값(arguments)으로 실행
    return originalSend.apply(this, arguments);
  };
})();