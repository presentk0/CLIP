import styles from './Spinner.module.css'

export function Spinner({ label = '로딩 중...', showLabel = true }) {
  return (
    // role="status" 는 해당 영역을 스크린 리더가 내용이 바뀌면 자동으로 읽어줌
    <span className={styles.loading} role="status">
      <span 
      className={styles.spinner} 
      // 스피너는 장식이라 읽지 말라고 하기
      aria-hidden="true" />
      {/* sr-only 써서 눈에는 안 보이지만, 스크린 리더는 읽을 수 있게 만들기 */}
      {showLabel ? label : <span className="sr-only">{label}</span>}
    </span>
  );
}

export function TestSpinner({ 
  label = '로딩 중...', 
  showLabel = true, 
  overlay = true 
}) {
  const content = (
    <span className={styles['test-loading']} role="status">
      <span className={styles['test-spinner']} aria-hidden="true" />
      {showLabel ? label : <span className={styles['test-sr-only']}>{label}</span>}
    </span>
  );

  if (!overlay) return content;
  return (
    <div className={styles['test-overlay']}>{content}</div>
  );
}