import React from 'react';import{createRoot}from'react-dom/client';import App from './App';import './styles.css';
import {HashRouter} from 'react-router-dom';
createRoot(document.getElementById('root')!).render(<React.StrictMode><HashRouter><App/></HashRouter></React.StrictMode>);
if('serviceWorker' in navigator)window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}));
