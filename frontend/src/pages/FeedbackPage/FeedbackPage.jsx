import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitFeedback } from '../../services/feedback';
import styles from './FeedbackPage.module.css';
import { log } from '../../utils/logger';
import frog from '../../imgs/image_809.png';


const SCORE_LABELS = {
  1: '어색했다',
  5: '자연스러웠다',
};

const FeedbackPage = () => {
  const navigate = useNavigate();
  const [score, setScore] = useState(0);
  const [goodPoint, setGoodPoint] = useState('');
  const [improvePoint, setImprovePoint] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 그냥 나가기 (X 버튼)
  const handleClose = () => {
    navigate('/'); // 홈으로
  };

  // 제출하고 나가기
  const handleSubmit = async () => {
    if (score === 0) {
      alert('만족도를 선택해주세요.');
      return;
    }
  if (!goodPoint.trim()) {
    alert('가장 좋았던 점을 입력해주세요.');
    return;
  }
  if (!improvePoint.trim()) {
    alert('가장 불편했던 점을 입력해주세요.');
    return;
  }
    try {
      setIsSubmitting(true);
      const result = await submitFeedback({
        satisfactionScore: score,
        goodPoint,
        improvePoint,
      });

      // apiFetch가 success: false면 throw하므로 여기 오면 성공
      alert(result?.message || '소중한 의견 감사합니다!');
      navigate('/');
    } catch (error) {
      log.debug('피드백 제출 실패:', error);
      alert(error.message || '제출에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
  <div className={styles.container}>
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
            onClick={() => setScore(num)}
          >
            <svg
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
            <span className={`${styles.card7} ${isActive ? styles.active : ''}`}>
              {num}
            </span>
            <span className={styles.scoreLabel}>
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




        {/* <div className={styles.header}>
          <img src="/frog.png" alt="frog" className={styles.icon} />
          <h2>여러분의 경험은 어땠나요?</h2>
          <p>잠깐, 30초만 들려주세요</p>
        </div> */}

        {/* Q1 */}
        {/* <section className={styles.section}>
          <h3>
            <span className={styles.qNum}>01</span>
            영상 보는 흐름에 학습이 자연스럽게 녹아들었나요?
          </h3>
          <div className={styles.scoreBox}>
            {[1, 2, 3, 4, 5].map((num) => (
              <div key={num} className={styles.scoreItem}>
                <button
                  className={`${styles.scoreBtn} ${score === num ? styles.active : ''}`}
                  onClick={() => setScore(num)}
                >
                  {num}
                </button>
                {SCORE_LABELS[num] && (
                  <span className={styles.scoreLabel}>{SCORE_LABELS[num]}</span>
                )}
              </div>
            ))}
          </div>
        </section> */}

        {/* Q2 */}
        {/* <section className={styles.section}>
          <h3>
            <span className={styles.qNum}>02</span>
            딱 하나씩만 알려주세요
          </h3>

          <label className={styles.label}>가장 좋았던 점</label>
          <textarea
            className={styles.textarea}
            placeholder="퀴즈, 채팅에 상관 없이 작성해주세요"
            value={goodPoint}
            onChange={(e) => setGoodPoint(e.target.value)}
          />

          <label className={styles.label}>가장 불편했던 점</label>
          <textarea
            className={styles.textarea}
            placeholder="퀴즈, 채팅에 상관 없이 작성해주세요"
            value={improvePoint}
            onChange={(e) => setImprovePoint(e.target.value)}
          />
        </section>

        <button
          className={styles.submitBtn}
          onClick={handleSubmit}
          disabled={isSubmitting}
        >
          {isSubmitting ? '제출 중...' : '제출하고 마치기'}
        </button> */}
      {/* </div> */}