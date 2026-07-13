import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitFeedback } from '../../services/feedback';
import styles from './FeedbackPage.module.css';
import { log } from '../../utils/logger';
import frog from '../../imgs/image_809.png';
import { useLocation } from 'react-router-dom';
import { saveUserData } from '../../utils/userStorage';
import { Toast } from '../../contexts/Toast';

const SCORE_LABELS = {
  1: '어색했다',
  5: '자연스러웠다',
};

const FeedbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [score, setScore] = useState(0);
  const [goodPoint, setGoodPoint] = useState('');
  const [improvePoint, setImprovePoint] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 팝업 메시지
  const [popupMessage, setPopupMessage] = useState('');
  const [popupCount, setPopupCount] = useState(0);

  const showPopup = (msg) => {
    setPopupMessage(msg);
    setPopupCount(prev => prev + 1);
  };

  // 어디로 가야 할지 받기
  const returnTo = location.state?.returnTo ?? '/';

  // 다음 페이지 이동 결정 함수
  const goToNext = () => {
    if (typeof returnTo === 'number') {
      navigate(returnTo);
    } else {
      navigate(returnTo, { replace: true });
    }
  };

  // 그냥 나가기 (X 버튼)
  const handleClose = () => {
    goToNext();
  };

  // 제출하고 나가기
  const handleSubmit = async () => {
    if (score === 0) {
      showPopup('만족도를 선택해주세요.');
      return;
    }
    if (!goodPoint.trim()) {
      showPopup('가장 좋았던 점을 입력해주세요.');
      return;
    }
    if (!improvePoint.trim()) {
      showPopup('가장 불편했던 점을 입력해주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      const result = await submitFeedback({
        satisfactionScore: score,
        goodPoint,
        improvePoint,
      });

      // API 제출 성공 시점에 로컬 스토리지에 제출 완료 기록
      // 로컬 저장은 try/catch로 분리 (실패해도 진행)
      try {
        await saveUserData('submittedFeedback', { hasSubmittedFeedback: true });
      } catch (saveError) {
        // 어차피 서버에 저장됐으니 문제없음
        log.debug('로컬 저장 실패 (무시 가능)', saveError);
      }
      showPopup(result?.message || '소중한 의견 감사합니다!');
      goToNext();
    } catch (error) {
      log.debug('피드백 제출 실패', error);
      showPopup(error.message || '제출에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.container}>
      {popupMessage && 
        <Toast 
        message={popupMessage}
        count={popupCount}
        onClose={() => setPopupMessage('')}
        />
      }

      <button className={styles.closeBtn} onClick={handleClose}>x</button>

      <div className={styles.frog1}>
        <div 
        className={styles.frog2}
        style={{ backgroundImage: `url(${frog})` }}
        ></div>
      </div>

      <div className={styles.card}>여러분의 경험은 어땠나요?</div>
      <div className={styles.card2}>잠깐, 30초만 들려주세요</div>

      <div className={styles.questionRow}>
        <span className={styles.card3}>01</span>
        <span className={styles.card4}>영상 보는 흐름에 학습이 자연스럽게 녹아들었나요?</span>
      </div>

      <div className={styles.card5}>
        {[1, 2, 3, 4, 5].map((num) => {
          const isActive = score === num;
          return (
            <button
            key={num}
            type="button"
            className={styles.scoreItem}
            aria-label={`${num}점, 5점 중`}
            aria-pressed={isActive}
            onClick={() => setScore(num)}
            >
              <svg
              aria-hidden="true"
              className={styles.scoreSvg}
              xmlns="http://www.w3.org/2000/svg"
              width="46" height="46" viewBox="0 0 46 46" fill="none"
              >
                <circle 
                cx="23" cy="23" r="22.5" 
                fill={isActive ? "#DBF3E7" : "white"} 
                stroke={isActive ? "#01CF8A" : "#D4CDB8"}
                />
              </svg>

              <span 
              aria-hidden="true"
              className={`${styles.card7} ${isActive ? styles.active : ''}`}>
                {num}
              </span>

              <span 
              aria-hidden="true"
              className={styles.scoreLabel}>
                {num === 1 ? '어색했다' : num === 5 ? '자연스러웠다' : '\u00A0'}
              </span>
            </button>
          );
        })}
      </div>

      <div className={styles.questionRow}>
        <span className={styles.card10}>02</span>
        <span className={styles.card11}>딱 하나씩만 알려주세요</span>
      </div>

      <div className={styles.card12}>가장 좋았던 점</div>

      <div className={styles.card13}>
        <textarea 
          className={styles.card14}
          placeholder="퀴즈, 채팅에 상관 없이 작성해주세요"
          value={goodPoint}
          onChange={(e) => setGoodPoint(e.target.value)}
        />
      </div>

      <div className={styles.card15}>가장 불편했던 점</div>

      <div className={styles.card16}>
        <textarea 
          className={styles.card17}
          placeholder="퀴즈, 채팅에 상관 없이 작성해주세요"
          value={improvePoint}
          onChange={(e) => setImprovePoint(e.target.value)}
        />
      </div>

      <button
        className={styles.card18}
        onClick={handleSubmit}
        disabled={isSubmitting}
      >
        <div className={styles.card19}>
          {isSubmitting ? '제출 중...' : '제출하고 마치기'}
        </div>
      </button>
    </div>
  );
};

export default FeedbackPage;