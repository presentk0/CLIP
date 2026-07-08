/* global chrome */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { log } from '../../utils/logger';
import styles from './TermsAgreementPage.module.css';


const TERMS_URL = 'https://presentk0.github.io/clipzy-legal/terms.html';
const PRIVACY_URL = 'https://presentk0.github.io/clipzy-legal/privacy';

export default function TermsAgreementPage() {
  const navigate = useNavigate();

  const [agreements, setAgreements] = useState({
    age: false,
    terms: false,
    privacy: false,
  });

  const [hasViewed, setHasViewed] = useState({
  terms: false,
  privacy: false,
});

  const allAgreed = agreements.age && agreements.terms && agreements.privacy;

  const toggleOne = (key) => {
    setAgreements((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleAll = () => {
    const next = !allAgreed;
    setAgreements({ age: next, terms: next, privacy: next });
  };

  // 외부 링크 안전하게 열기 (보안 화이트리스트)
  const openExternalLink = (url) => {
    const allowedUrls = [TERMS_URL, PRIVACY_URL];
    if (!allowedUrls.includes(url)) {
      log.error('허용되지 않은 URL');
      return;
    }

    if (typeof chrome !== 'undefined' && chrome.tabs) {
      chrome.tabs.create({ url });
    } else {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  const handleConfirm = () => {
    if (!allAgreed) return;
    // API 호출 없이 다음 단계로
    navigate('/onboarding/level', { replace: true });
  };

  const handleBack = () => navigate('/login', { replace: true });

  return (
    <div className={styles.agreement}>
      <div className={styles.agreement2}>
        <div className={styles.agreement3}>
          <button 
          onClick={handleBack}
          className={styles.agreement4}>
            <svg 
            className={styles.agreement5}
            xmlns="http://www.w3.org/2000/svg" width="18" height="19" viewBox="0 0 18 19" fill="none">
              <path d="M15.1776 0.302198C15.5553 -0.100732 16.1681 -0.100733 16.5458 0.302198C16.9232 0.705146 16.9234 1.35833 16.5458 1.76118L9.81627 8.93892L16.8202 16.4116C17.1979 16.8145 17.1979 17.4676 16.8202 17.8706C16.4425 18.2731 15.8306 18.2731 15.453 17.8706L8.44811 10.3979L1.65123 17.6499C1.27352 18.0528 0.660772 18.0528 0.283068 17.6499C-0.0943871 17.247 -0.0943246 16.5938 0.283068 16.1909L7.07994 8.93892L0.558458 1.98189C0.180747 1.57895 0.180747 0.924854 0.558458 0.521924C0.936176 0.119297 1.549 0.119136 1.92662 0.521924L8.44811 7.47993L15.1776 0.302198Z" fill="#454440"/>
            </svg>
          </button>

          <div className={styles.agreement6}>
            <div className={styles.agreement7}>
              <svg 
              className={styles.agreement8}
              xmlns="http://www.w3.org/2000/svg" width="128" height="24" viewBox="0 0 128 24" fill="none">
                <path d="M28.4773 0C31.2211 0 33.6069 1.88063 34.3056 4.42123C38.3445 5.13258 41.4119 8.65481 41.412 12.8551V15.4305C41.4117 20.1556 37.5438 23.9998 32.7894 24H16.8208C13.7361 24 11.2138 21.4932 11.2138 18.4276C11.214 15.362 13.7362 12.8551 16.8208 12.8551H23.6376C25.0009 12.8551 26.2792 12.1771 27.046 11.0423C27.5744 10.2469 28.6472 10.0447 29.4309 10.5693C30.2148 11.0943 30.4367 12.1604 29.9086 12.9395C28.5112 15.0227 26.1598 16.2773 23.6376 16.2773H16.8208C15.6279 16.2773 14.6557 17.2421 14.6556 18.4276C14.6556 19.6132 15.6278 20.5795 16.8208 20.5795H32.7728C35.6183 20.579 37.9517 18.2754 37.952 15.4305V12.8551C37.9518 10.027 35.6354 7.70657 32.7728 7.70613C31.8184 7.70613 31.0503 6.94433 31.0503 5.99586C31.0503 4.57324 29.8921 3.42072 28.4607 3.42054C27.0291 3.42054 25.8694 4.57313 25.8694 5.99586C25.8694 6.94433 25.1029 7.70613 24.1485 7.70613H17.2468C16.2925 7.70613 15.526 6.94433 15.526 5.99586C15.526 4.57313 14.3662 3.42054 12.9347 3.42054C11.5033 3.42068 10.3451 4.57322 10.3451 5.99586C10.3451 6.94433 9.57692 7.70613 8.62258 7.70613C5.77697 7.70653 3.44186 10.01 3.44171 12.8551V15.4305C3.44203 18.2585 5.76004 20.5791 8.62258 20.5795C9.57694 20.5795 10.3451 21.3412 10.3451 22.2897C10.345 23.2381 9.57685 24 8.62258 24C3.86836 23.9996 0.000324386 20.1554 0 15.4305V12.8551C0.000138613 8.63796 3.08469 5.13267 7.10643 4.42123C7.80513 1.88071 10.157 0.000120259 12.9347 0C15.7126 0 17.9798 1.81275 18.7297 4.2856H22.6823C23.4322 1.81277 25.7336 2.86267e-05 28.4773 0ZM54.3983 4.99683C55.489 4.99683 56.4953 5.14847 57.4156 5.45334C58.3357 5.74128 59.1535 6.18261 59.8521 6.75837C60.5677 7.31727 61.1299 8.02918 61.573 8.85899C62.3053 10.2477 61.4188 11.7205 59.7489 11.7205C58.0798 11.7201 57.9603 10.4674 56.9213 9.58677C56.6317 9.31581 56.2735 9.12846 55.8645 8.99297L55.8462 8.97643C55.4377 8.84119 54.9951 8.77306 54.5015 8.77298C53.6326 8.77298 52.8818 8.99319 52.2514 9.4164C51.6208 9.83984 51.1427 10.4679 50.8018 11.264C50.4782 12.0598 50.3075 12.9915 50.3075 14.1767C50.3075 15.3622 50.4793 16.3613 50.8201 17.1573C51.161 17.9533 51.6545 18.5636 52.268 18.9701C52.8986 19.3766 53.6494 19.5788 54.5015 19.5788C54.995 19.5787 55.4377 19.5122 55.8462 19.377C56.2551 19.2415 56.5963 19.0372 56.903 18.7832C57.9425 17.9025 58.1136 16.6497 59.7323 16.6495C61.351 16.6495 62.2884 18.1223 61.5563 19.511C61.1133 20.3407 60.534 21.0358 59.8355 21.6116C59.1367 22.1874 58.3176 22.6117 57.3973 22.9166C56.4603 23.2213 55.472 23.3731 54.3817 23.3731C52.7627 23.3731 51.3131 23.0176 50.0179 22.3063C48.7402 21.5781 47.7354 20.5449 46.9856 19.1735C46.2529 17.8017 45.8773 16.1414 45.8772 14.1767C45.8772 12.212 46.2524 10.5518 47.0023 9.17988C47.7521 7.80811 48.7745 6.77502 50.0695 6.06368C51.3477 5.35234 52.7964 4.99685 54.3983 4.99683ZM65.7819 5.23336C66.9919 5.23336 67.9638 6.19924 67.9638 7.40179V18.7319C67.9638 19.2231 68.3566 19.6135 68.8508 19.6135H73.6905C74.6617 19.6137 75.463 20.3928 75.463 21.3751C75.4629 22.3572 74.6787 23.1364 73.6905 23.1366H66.2762L66.2596 23.1201C64.794 23.1199 63.6017 21.9334 63.6017 20.4769V7.40179C63.6017 6.21641 64.5722 5.23373 65.7819 5.23336ZM78.9396 5.23336C80.1323 5.23363 81.1198 6.19942 81.1198 7.40179V20.9682C81.1198 22.1536 80.1494 23.1363 78.9396 23.1366C77.7296 23.1366 76.7578 22.1707 76.7578 20.9682V7.40179C76.7578 6.21618 77.7296 5.23336 78.9396 5.23336ZM125.89 5.2499C127.492 5.25002 128.514 6.96084 127.731 8.34955L123.078 16.7686C122.942 17.0225 122.856 17.3278 122.856 17.6154V20.9847C122.856 22.1701 121.886 23.1363 120.693 23.1366C119.5 23.1366 118.528 22.1703 118.528 20.9847V17.6154C118.528 17.3277 118.459 17.0226 118.306 16.7686L113.655 8.34955H113.688C112.921 6.96078 113.927 5.2499 115.529 5.2499C116.33 5.24991 117.045 5.6914 117.403 6.40276L120.59 12.6699C120.593 12.6736 120.66 12.736 120.709 12.736C120.76 12.736 120.795 12.7037 120.829 12.6699L124.015 6.40276C124.373 5.69139 125.106 5.2499 125.89 5.2499ZM91.2768 5.23336C92.6232 5.23336 93.7834 5.48735 94.7718 6.01241C95.7601 6.53745 96.5099 7.24939 97.0552 8.18084C97.5834 9.11234 97.8557 10.1798 97.8557 11.4161C97.8556 12.6524 97.5838 13.7369 97.0386 14.6514C96.4932 15.5659 95.7256 16.277 94.7202 16.7851C94.1239 17.0899 93.4594 17.293 92.7098 17.4285C92.4031 17.4793 92.0105 17.5476 91.4483 17.5476C90.4939 17.5476 89.6924 16.9033 89.6242 15.9548C89.5561 15.0233 90.2385 14.3283 91.2269 14.142C91.8913 14.0232 92.4711 13.4993 92.5733 13.3977C92.7437 13.2452 92.8801 13.0583 92.9994 12.8551C93.2207 12.4488 93.3388 11.9578 93.3389 11.4161C93.3389 10.8741 93.2209 10.382 92.9994 9.97546C92.7779 9.56929 92.4368 9.24784 91.9941 9.02771C91.551 8.80752 91.0041 8.68863 90.3565 8.68863H89.0966C88.6024 8.68863 88.2096 9.07905 88.2096 9.57023V20.9169C88.2096 22.1022 87.239 23.0849 86.0294 23.0853V23.1201C84.8365 23.1201 83.8476 22.1541 83.8475 20.9516V7.8765C83.8475 6.42003 85.04 5.23358 86.5054 5.23336H91.2768ZM110.654 5.21682C111.625 5.21682 112.426 6.01293 112.426 6.97836V7.11399C112.426 7.48645 112.307 7.84135 112.085 8.14611L103.889 19.3092C103.804 19.4277 103.889 19.597 104.025 19.597H110.637C111.609 19.597 112.41 20.3761 112.41 21.3585C112.409 22.3406 111.626 23.1201 110.637 23.1201H99.9344C98.9635 23.1197 98.1623 22.3235 98.1619 21.3585V21.2229C98.1619 20.8503 98.2816 20.494 98.5031 20.1891L106.701 9.02771C106.786 8.90928 106.701 8.74027 106.565 8.7399H99.9527C98.9813 8.7399 98.1803 7.96064 98.1802 6.97836C98.1802 5.996 98.9642 5.21682 99.9527 5.21682H110.654Z" fill="#01CF8A"/>
              </svg>
            </div>

            <div className={styles.agreement9}>
              <p className={styles.agreement10}>서비스 가입을 위해 이용약관에 동의해주세요</p>
            </div>
          </div>
        </div>

        <div className={styles.agreement11}>
          <div className={styles.agreement12}>
            <div className={styles.agreement13}>
              <div className={styles.agreement14}>
                <div className={styles.agreement15}>


                  <button className={styles.agreement18}
                  type="button"
                  onClick={() => toggleOne('age')}
                  aria-pressed={agreements.age}
                  >
                    {agreements.age 
                    ? <div className={styles.agreement16}>
                    <svg 
                    className={styles.agreement17}
                    xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
                    </svg>
                  </div>
                    : <svg 
                    className={styles.agreement22}
                    xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <g clip-path="url(#clip0_935_1154)">
                        <path d="M23.3 12C23.3 5.75913 18.2408 0.699951 12 0.699951C5.75918 0.699951 0.699997 5.75913 0.699997 12C0.699997 18.2408 5.75918 23.2999 12 23.2999C18.2408 23.2999 23.3 18.2408 23.3 12Z" fill="#FEFDF9" stroke="#A5E7D2" stroke-width="1.4"/>
                      </g>
                      <defs>
                        <clipPath id="clip0_935_1154">
                          <rect width="24" height="24" fill="white"/>
                        </clipPath>
                      </defs>
                    </svg>}
                    <p className={styles.agreement19}>만 14세 이상입니다 (필수)</p>
                  </button>
                </div>
              </div>



              <div className={styles.agreement14}>
                <div className={styles.agreement15}>


                  <button className={styles.agreement18}
                  type="button"
                  onClick={() => {
                    // 처음 동의하려는 거면 링크 강제로 열기
                    if (!hasViewed.terms && !agreements.terms) {
                      openExternalLink(TERMS_URL);
                      setHasViewed((prev) => ({ ...prev, terms: true }));
                    }
                    toggleOne('terms');
                  }}
                  aria-pressed={agreements.terms}
                  >
                    {agreements.terms
                    ? <div className={styles.agreement16}>
                    <svg 
                    className={styles.agreement17}
                    xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
                    </svg>
                  </div>
                    : <svg 
                    className={styles.agreement22}
                    xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <g clip-path="url(#clip0_935_1154)">
                        <path d="M23.3 12C23.3 5.75913 18.2408 0.699951 12 0.699951C5.75918 0.699951 0.699997 5.75913 0.699997 12C0.699997 18.2408 5.75918 23.2999 12 23.2999C18.2408 23.2999 23.3 18.2408 23.3 12Z" fill="#FEFDF9" stroke="#A5E7D2" stroke-width="1.4"/>
                      </g>
                      <defs>
                        <clipPath id="clip0_935_1154">
                          <rect width="24" height="24" fill="white"/>
                        </clipPath>
                      </defs>
                    </svg>}
                    <p className={styles.agreement19}>서비스 이용약관 동의 (필수)</p>
                  </button>
                </div>
              </div>

              <div className={styles.agreement14}>
                <div className={styles.agreement15}>


                  <button className={styles.agreement18}
                  type="button"
                  onClick={() => {
                    // 처음 동의하려는 거면 링크 강제로 열기
                    if (!hasViewed.privacy && !agreements.privacy) {
                      openExternalLink(PRIVACY_URL);
                      setHasViewed((prev) => ({ ...prev, privacy: true }));
                    }
                    toggleOne('privacy');
                  }}
                  aria-pressed={agreements.privacy}
                >
                    {agreements.privacy
                    ? 
                    <div className={styles.agreement16}>
                    <svg 
                    className={styles.agreement17}
                    xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
                    </svg>
                  </div>
                    : <svg 
                    className={styles.agreement22}
                    xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <g clip-path="url(#clip0_935_1154)">
                        <path d="M23.3 12C23.3 5.75913 18.2408 0.699951 12 0.699951C5.75918 0.699951 0.699997 5.75913 0.699997 12C0.699997 18.2408 5.75918 23.2999 12 23.2999C18.2408 23.2999 23.3 18.2408 23.3 12Z" fill="#FEFDF9" stroke="#A5E7D2" stroke-width="1.4"/>
                      </g>
                      <defs>
                        <clipPath id="clip0_935_1154">
                          <rect width="24" height="24" fill="white"/>
                        </clipPath>
                      </defs>
                    </svg>}
                    <p className={styles.agreement19}>개인정보 처리방침 동의 (필수)</p>
                  </button>
                </div>
              </div>
            </div>

            <button 
            className={styles.agreement21}
            type="button"
            onClick={toggleAll}
            aria-pressed={allAgreed}
            >
              {allAgreed 
              ? <div className={styles.agreement16}>
                    <svg 
                    className={styles.agreement17}
                    xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
                    </svg>
                  </div>
              : <svg 
              className={styles.agreement22}
              xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <g clip-path="url(#clip0_935_1154)">
                  <path d="M23.3 12C23.3 5.75913 18.2408 0.699951 12 0.699951C5.75918 0.699951 0.699997 5.75913 0.699997 12C0.699997 18.2408 5.75918 23.2999 12 23.2999C18.2408 23.2999 23.3 18.2408 23.3 12Z" fill="#FEFDF9" stroke="#A5E7D2" stroke-width="1.4"/>
                </g>
                <defs>
                  <clipPath id="clip0_935_1154">
                    <rect width="24" height="24" fill="white"/>
                  </clipPath>
                </defs>
              </svg>}
              

              <div className={styles.agreement23}>
                <p className={styles.agreement24}>모든 약관에 동의합니다</p>
              </div>
            </button>
          </div>

          <button 
          className={styles.agreement25}
          type="button"
          onClick={handleConfirm}
          disabled={!allAgreed}
          >
            <p className={styles.agreement26}>확인</p>
          </button>
        </div>
      </div>
    </div>
  );
}