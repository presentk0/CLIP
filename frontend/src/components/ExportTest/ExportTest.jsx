export function ExportTest() {
  // 테스트용 임시 데이터
  const testWords = [
    { word: 'boarding', partOfSpeech: '명사', meaning: '탑승 중' },
    { word: 'serendipity', partOfSpeech: '명사', meaning: '뜻밖의 행운' },
    { word: 'alternative', partOfSpeech: '명사', meaning: '대안' },
  ];

  // CSV 내보내기
  const exportToCsv = () => {
    const header = '단어,품사,뜻\n';
    const rows = testWords.map(w => 
      `${w.word},${w.partOfSpeech},${w.meaning}`
    ).join('\n');
    
    const csv = '\uFEFF' + header + rows;
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = '단어장.csv';
    a.click();
    
    URL.revokeObjectURL(url);
  };

  // 메모장 내보내기
  const exportToTxt = () => {
    const text = testWords.map(w => 
      `${w.word} (${w.partOfSpeech}): ${w.meaning}`
    ).join('\n');
    
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = '단어장.txt';
    a.click();
    
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ padding: 20, display: 'flex', gap: 10 }}>
      <button onClick={exportToCsv}>CSV 내보내기</button>
      <button onClick={exportToTxt}>TXT 내보내기</button>
    </div>
  );
}