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

// 사용 방법
{/* <Spinner />                              // "로딩 중..." 보임
<Spinner showLabel={false} />            // 스피너만 보임
<Spinner label="데이터 가져오는 중..." />  // 텍스트 변경 */}