import { useEffect } from "react";
import styles from './Toast.module.css';

export function Toast({ message, count, onClose, duration = 2500 }) {
  useEffect(() => {
    if (message) {
      const timer = setTimeout(onClose, duration);
      return () => clearTimeout(timer);
    }
  }, [message, count, onClose, duration]);
  
  if (!message) return null;
  
  return (
    <p role="alert" key={count} className={styles.popup}>
      {message}
    </p>
  );
}