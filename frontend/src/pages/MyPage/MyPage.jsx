import styles from './MyPage.module.css'

export function MyPage () {
  return (
    <div className={styles.container}>
      <div className={styles.header} />
      <div className={styles.today}></div>
    </div>
  );
}