import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import Recorder from './Recorder.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Recorder />
  </StrictMode>,
)