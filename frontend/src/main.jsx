import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { BillingProvider } from './context/BillingContext.jsx';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <BillingProvider><App /></BillingProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
