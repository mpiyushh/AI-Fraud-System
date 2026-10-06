/**
 * SENTINEL AI - Modern UPI Transaction & Fraud Analysis Controller
 * Manages dynamic UPI payment methods, real-time validations, REST API ingestion,
 * explainable AI (SHAP) visualization, and personal security log monitoring.
 */

const SentinelTransaction = (() => {
  // Current Session & Form State
  let currentUser = {
    userId: 'USR-101',
    fullName: 'Alexandre V.',
    primaryLocation: 'London, UK',
    cardMasked: 'Visa •• 4912',
    bank: 'Chase',
    isBlacklisted: false
  };

  let activeMethod = 'upi-qr';
  let currencySymbol = '₹';
  let currencyCode = 'INR';
  let recentTransactions = [];
  let userProfiles = [];
  let isAnalyzing = false;

  // DOM Elements Cache
  const DOM = {};

  function init() {
    cacheElements();
    setupNavigation();
    setupPaymentMethodTabs();
    setupFormValidation();
    setupAccountSwitcher();
    setupQrScannerModal();
    setupQuickFillChips();
    setupSecurityLogEvents();
    setupFormSubmit();

    // Initial data fetches
    checkBackendHealth();
    loadUserProfiles();
    refreshSecurityLog();

    // Set polling interval for security log (every 10s)
    setInterval(() => {
      refreshSecurityLog(false);
    }, 10000);
  }

  function cacheElements() {
    DOM.serverPulseDot = document.getElementById('serverPulseDot');
    DOM.serverStatusText = document.getElementById('serverStatusText');
    DOM.sessionUserBadge = document.getElementById('sessionUserBadge');
    DOM.sessionUserName = document.getElementById('sessionUserName');
    DOM.sessionUserBank = document.getElementById('sessionUserBank');
    DOM.sessionUserCard = document.getElementById('sessionUserCard');
    DOM.btnSwitchUser = document.getElementById('btnSwitchUser');
    DOM.btnLogout = document.getElementById('btnLogout');

    DOM.paymentMethodCards = document.querySelectorAll('.payment-method-card');
    DOM.dynamicInputPanels = document.querySelectorAll('.payment-method-panel');

    DOM.formTransaction = document.getElementById('formTransaction');
    DOM.btnAnalyze = document.getElementById('btnAnalyze');
    DOM.txLocation = document.getElementById('txLocation');
    DOM.txVpnOrTor = document.getElementById('txVpnOrTor');
    DOM.currencySelector = document.getElementById('currencySelector');

    // Result card elements
    DOM.resultContainer = document.getElementById('fraudResultContainer');
    DOM.resultEmptyState = document.getElementById('fraudResultPlaceholder');
    DOM.resultCard = document.getElementById('fraudResultCard');
    DOM.resultRiskScore = document.getElementById('resRiskScore');
    DOM.resultScoreRing = document.getElementById('resScoreRing');
    DOM.resultVerdictBadge = document.getElementById('resVerdictBadge');
    DOM.resultVerdictTitle = document.getElementById('resVerdictTitle');
    DOM.resultThreatSignal = document.getElementById('resThreatSignal');
    DOM.resultRecipient = document.getElementById('resRecipient');
    DOM.resultAmount = document.getElementById('resAmount');
    DOM.resultLatency = document.getElementById('resLatency');
    DOM.resultTxId = document.getElementById('resTxId');
    DOM.resultAttributions = document.getElementById('resAttributionsList');
    DOM.btnResetForm = document.getElementById('btnResetForm');

    // Security Log
    DOM.securityLogList = document.getElementById('securityLogList');
    DOM.securityLogEmpty = document.getElementById('securityLogEmpty');
    DOM.securityLogCount = document.getElementById('securityLogCount');
    DOM.btnRefreshLog = document.getElementById('btnRefreshLog');
    DOM.logFilterTabs = document.querySelectorAll('.log-filter-tab');

    // Modals
    DOM.modalUserSwitch = document.getElementById('modalUserSwitch');
    DOM.userListContainer = document.getElementById('userListContainer');
    DOM.btnCloseUserModal = document.getElementById('btnCloseUserModal');
    DOM.modalQrScanner = document.getElementById('modalQrScanner');
    DOM.btnCloseQrModal = document.getElementById('btnCloseQrModal');
    DOM.btnOpenQrScanner = document.getElementById('btnOpenQrScanner');
    DOM.modalTxDetails = document.getElementById('modalTxDetails');
    DOM.btnCloseTxModal = document.getElementById('btnCloseTxModal');
    DOM.modalTxContent = document.getElementById('modalTxContent');
  }

  // ==========================================================
  //  1. BACKEND HEALTH & SESSION INITIALIZATION
  // ==========================================================
  async function checkBackendHealth() {
    try {
      const res = await SentinelAPI.checkHealth();
      if (DOM.serverPulseDot && DOM.serverStatusText) {
        DOM.serverPulseDot.className = 'pulse-dot online';
        DOM.serverStatusText.textContent = 'Neural Core Online (Port 8080)';
      }
      return res;
    } catch (err) {
      if (DOM.serverPulseDot && DOM.serverStatusText) {
        DOM.serverPulseDot.className = 'pulse-dot offline';
        DOM.serverStatusText.textContent = 'Backend Offline';
      }
    }
  }

  async function loadUserProfiles() {
    try {
      const res = await SentinelAPI.getUsers();
      if (res && res.data && Array.isArray(res.data) && res.data.length > 0) {
        userProfiles = res.data;
        // If current user is in list, sync details
        const found = userProfiles.find(u => u.userId === currentUser.userId);
        if (found) {
          currentUser.fullName = found.fullName;
          currentUser.primaryLocation = found.primaryLocation || 'London, UK';
          currentUser.isBlacklisted = !!found.isBlacklisted;
        } else {
          // Select first available profile
          const first = userProfiles[0];
          currentUser.userId = first.userId;
          currentUser.fullName = first.fullName;
          currentUser.primaryLocation = first.primaryLocation || 'London, UK';
          currentUser.isBlacklisted = !!first.isBlacklisted;
        }
        updateUserSessionUI();
      }
    } catch (err) {
      console.warn('Could not fetch user profiles from API, using active session state.');
    }
  }

  function updateUserSessionUI() {
    if (DOM.sessionUserName) DOM.sessionUserName.textContent = currentUser.fullName;
    if (DOM.sessionUserBadge) DOM.sessionUserBadge.textContent = currentUser.userId;
    if (DOM.sessionUserBank) DOM.sessionUserBank.textContent = currentUser.bank;
    if (DOM.sessionUserCard) DOM.sessionUserCard.textContent = currentUser.cardMasked;

    // Location default sync if standard location input is present
    if (DOM.txLocation && currentUser.primaryLocation) {
      const matchOpt = Array.from(DOM.txLocation.options).find(opt => opt.value.includes(currentUser.primaryLocation.split(',')[0]));
      if (matchOpt) {
        DOM.txLocation.value = matchOpt.value;
      }
    }
  }

  // ==========================================================
  //  2. NAVIGATION & TAB SWITCHING
  // ==========================================================
  function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const viewSections = document.querySelectorAll('.view-section');

    navItems.forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const targetView = item.getAttribute('data-view');
        if (!targetView) return;

        navItems.forEach(n => n.classList.remove('active'));
        item.classList.add('active');

        viewSections.forEach(section => {
          if (section.id === `view-${targetView}`) {
            section.classList.remove('hidden');
          } else {
            section.classList.add('hidden');
          }
        });

        // Trigger specific refreshes
        if (targetView === 'history' || targetView === 'alerts') {
          refreshSecurityLog(true);
        }
      });
    });
  }

  // ==========================================================
  //  3. DYNAMIC UPI PAYMENT METHODS
  // ==========================================================
  function setupPaymentMethodTabs() {
    DOM.paymentMethodCards.forEach(card => {
      card.addEventListener('click', () => {
        const method = card.getAttribute('data-method');
        if (!method || method === activeMethod) return;

        // Update active card state
        DOM.paymentMethodCards.forEach(c => c.classList.remove('active'));
        card.classList.add('active');
        activeMethod = method;

        // Switch visible input panel
        DOM.dynamicInputPanels.forEach(panel => {
          if (panel.getAttribute('data-panel') === method) {
            panel.classList.remove('hidden');
            panel.classList.add('panel-active');
          } else {
            panel.classList.add('hidden');
            panel.classList.remove('panel-active');
          }
        });

        // Reset errors
        clearFormErrors();
      });
    });

    // Currency selector switch
    if (DOM.currencySelector) {
      DOM.currencySelector.addEventListener('change', (e) => {
        currencyCode = e.target.value;
        currencySymbol = currencyCode === 'INR' ? '₹' : (currencyCode === 'USD' ? '$' : '€');
        document.querySelectorAll('.currency-prefix').forEach(el => {
          el.textContent = currencySymbol;
        });
      });
    }
  }

  // ==========================================================
  //  4. FORM VALIDATION & QUICK FILL
  // ==========================================================
  function setupFormValidation() {
    // Live input formatting & real-time feedback

    // Phone number field
    const phoneInput = document.getElementById('phoneRecipientNumber');
    if (phoneInput) {
      phoneInput.addEventListener('input', (e) => {
        let val = e.target.value.replace(/\D/g, '');
        if (val.length > 10) val = val.substring(0, 10);
        e.target.value = val;
        validatePhoneField(phoneInput);
      });
    }

    // UPI ID field
    const upiInput = document.getElementById('upiIdInput');
    if (upiInput) {
      upiInput.addEventListener('input', (e) => {
        e.target.value = e.target.value.trim().toLowerCase();
        validateUpiIdField(upiInput);
      });
    }

    // IFSC Code field (auto uppercase)
    const ifscInput = document.getElementById('bankIfscCode');
    if (ifscInput) {
      ifscInput.addEventListener('input', (e) => {
        e.target.value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').substring(0, 11);
        validateIfscField(ifscInput);
      });
    }

    // Bank Account Number (digits only)
    const accountInput = document.getElementById('bankAccountNumber');
    if (accountInput) {
      accountInput.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').substring(0, 18);
        validateAccountNumberField(accountInput);
      });
    }
  }

  function validatePhoneField(input) {
    const val = input.value.trim();
    const phoneRegex = /^[6-9]\d{9}$/;
    const errEl = document.getElementById('errPhoneNumber');
    if (!val) {
      setError(input, errEl, 'Mobile number is required');
      return false;
    } else if (val.length !== 10 || !phoneRegex.test(val)) {
      setError(input, errEl, 'Enter a valid 10-digit Indian mobile number (starts with 6-9)');
      return false;
    }
    clearError(input, errEl);
    return true;
  }

  function validateUpiIdField(input) {
    const val = input.value.trim();
    const upiRegex = /^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$/;
    const errEl = document.getElementById('errUpiId');
    if (!val) {
      setError(input, errEl, 'UPI ID is required');
      return false;
    } else if (!upiRegex.test(val)) {
      setError(input, errEl, 'Invalid UPI ID format (e.g., username@bank)');
      return false;
    }
    clearError(input, errEl);
    return true;
  }

  function validateIfscField(input) {
    const val = input.value.trim();
    const ifscRegex = /^[A-Z]{4}0[A-Z0-9]{6}$/;
    const errEl = document.getElementById('errBankIfsc');
    if (!val) {
      setError(input, errEl, 'IFSC code is required');
      return false;
    } else if (val.length !== 11 || !ifscRegex.test(val)) {
      setError(input, errEl, 'Invalid 11-character IFSC (e.g., HDFC0001234)');
      return false;
    }
    clearError(input, errEl);
    return true;
  }

  function validateAccountNumberField(input) {
    const val = input.value.trim();
    const errEl = document.getElementById('errBankAccount');
    if (!val) {
      setError(input, errEl, 'Account number is required');
      return false;
    } else if (val.length < 9 || val.length > 18) {
      setError(input, errEl, 'Account number must be 9 to 18 digits');
      return false;
    }
    clearError(input, errEl);
    return true;
  }

  function setError(input, errElement, message) {
    if (input) input.classList.add('input-error');
    if (errElement) {
      errElement.textContent = message;
      errElement.classList.remove('hidden');
    }
  }

  function clearError(input, errElement) {
    if (input) input.classList.remove('input-error');
    if (errElement) {
      errElement.textContent = '';
      errElement.classList.add('hidden');
    }
  }

  function clearFormErrors() {
    document.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
    document.querySelectorAll('.field-error-msg').forEach(el => {
      el.textContent = '';
      el.classList.add('hidden');
    });
  }

  function setupQuickFillChips() {
    // UPI Quick handle chips
    document.querySelectorAll('.vpa-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const handle = chip.getAttribute('data-handle');
        const upiInput = document.getElementById('upiIdInput');
        if (upiInput) {
          const currentVal = upiInput.value.split('@')[0] || 'customer';
          upiInput.value = `${currentVal}${handle}`;
          validateUpiIdField(upiInput);
        }
      });
    });

    // Preset Amount chips
    document.querySelectorAll('.amount-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const amt = chip.getAttribute('data-amount');
        const activeAmountInput = document.querySelector(`.payment-method-panel[data-panel="${activeMethod}"] .amount-input`);
        if (activeAmountInput) {
          activeAmountInput.value = amt;
        }
      });
    });
  }

  // ==========================================================
  //  5. QR SCANNER MODAL (NON-FAKE PLACEHOLDER)
  // ==========================================================
  function setupQrScannerModal() {
    if (DOM.btnOpenQrScanner) {
      DOM.btnOpenQrScanner.addEventListener('click', () => {
        openModal(DOM.modalQrScanner);
      });
    }

    if (DOM.btnCloseQrModal) {
      DOM.btnCloseQrModal.addEventListener('click', () => {
        closeModal(DOM.modalQrScanner);
      });
    }

    // Demo QR simulation chips inside the scanner modal
    document.querySelectorAll('.qr-sample-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const sampleUpi = btn.getAttribute('data-upi');
        const sampleName = btn.getAttribute('data-name');
        const sampleAmt = btn.getAttribute('data-amount');

        const qrUpiInput = document.getElementById('qrUpiInput');
        const qrAmountInput = document.getElementById('qrAmountInput');
        const qrRecipientInput = document.getElementById('qrRecipientName');

        if (qrUpiInput) qrUpiInput.value = sampleUpi;
        if (qrAmountInput) qrAmountInput.value = sampleAmt;
        if (qrRecipientInput) qrRecipientInput.value = sampleName;

        closeModal(DOM.modalQrScanner);
      });
    });
  }

  // ==========================================================
  //  6. SUBMIT & ANALYZE TRANSACTION
  // ==========================================================
  function setupFormSubmit() {
    if (DOM.formTransaction) {
      DOM.formTransaction.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (isAnalyzing) return;

        // 1. Validate based on active payment method
        const transactionData = extractAndValidateFormData();
        if (!transactionData) return;

        // 2. Set loading state
        setAnalyzingState(true);

        try {
          // 3. Call REST API POST /api/transactions
          const response = await SentinelAPI.submitTransaction(transactionData);

          if (response && response.data) {
            // 4. Render real fraud evaluation result
            renderFraudResult(response.data);
            // 5. Refresh personal security log
            await refreshSecurityLog(true);
            // Scroll to result on smaller screens
            DOM.resultContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
          } else {
            throw new Error(response.message || 'No evaluation data received from AI engine.');
          }
        } catch (err) {
          console.error('[Transaction Error]', err);
          showGlobalNotification(err.message || 'Failed to submit transaction for evaluation.', 'error');
        } finally {
          setAnalyzingState(false);
        }
      });
    }

    if (DOM.btnResetForm) {
      DOM.btnResetForm.addEventListener('click', () => {
        resetResultView();
      });
    }
  }

  function extractAndValidateFormData() {
    clearFormErrors();
    let isValid = true;

    let amount = 0;
    let recipient = '';
    let cardMasked = currentUser.cardMasked;
    let bank = currentUser.bank;

    if (activeMethod === 'upi-qr') {
      const qrUpi = document.getElementById('qrUpiInput');
      const qrRecipient = document.getElementById('qrRecipientName');
      const qrAmount = document.getElementById('qrAmountInput');
      const errQrUpi = document.getElementById('errQrUpi');
      const errQrAmount = document.getElementById('errQrAmount');

      const upiVal = qrUpi ? qrUpi.value.trim() : '';
      if (!upiVal) {
        setError(qrUpi, errQrUpi, 'Enter UPI ID or Mobile Number');
        isValid = false;
      }

      amount = parseFloat(qrAmount ? qrAmount.value : 0);
      if (isNaN(amount) || amount <= 0) {
        setError(qrAmount, errQrAmount, 'Enter a valid amount greater than ₹0');
        isValid = false;
      }

      recipient = qrRecipient && qrRecipient.value.trim() ? qrRecipient.value.trim() : (upiVal.split('@')[0] || 'UPI Merchant');
      cardMasked = `UPI •• ${upiVal.includes('@') ? upiVal : '+91 ' + upiVal}`;
      bank = 'NPCI / UPI Network';

    } else if (activeMethod === 'pay-number') {
      const phoneInput = document.getElementById('phoneRecipientNumber');
      const phoneName = document.getElementById('phoneRecipientName');
      const phoneAmount = document.getElementById('phoneAmountInput');
      const errAmount = document.getElementById('errPhoneAmount');

      if (!validatePhoneField(phoneInput)) isValid = false;

      amount = parseFloat(phoneAmount ? phoneAmount.value : 0);
      if (isNaN(amount) || amount <= 0) {
        setError(phoneAmount, errAmount, 'Enter a valid amount greater than ₹0');
        isValid = false;
      }

      const phoneVal = phoneInput.value.trim();
      recipient = phoneName && phoneName.value.trim() ? phoneName.value.trim() : `Contact +91 ${phoneVal}`;
      cardMasked = `UPI Mobile •• ${phoneVal.slice(-4)}`;
      bank = 'UPI / PhonePe / GPay';

    } else if (activeMethod === 'upi-id') {
      const upiInput = document.getElementById('upiIdInput');
      const upiName = document.getElementById('upiRecipientName');
      const upiAmount = document.getElementById('upiAmountInput');
      const errAmount = document.getElementById('errUpiAmount');

      if (!validateUpiIdField(upiInput)) isValid = false;

      amount = parseFloat(upiAmount ? upiAmount.value : 0);
      if (isNaN(amount) || amount <= 0) {
        setError(upiAmount, errAmount, 'Enter a valid amount greater than ₹0');
        isValid = false;
      }

      const upiVal = upiInput.value.trim();
      recipient = upiName && upiName.value.trim() ? upiName.value.trim() : upiVal;
      cardMasked = `VPA •• ${upiVal}`;
      bank = deriveBankFromVpa(upiVal);

    } else if (activeMethod === 'bank-transfer') {
      const holderInput = document.getElementById('bankAccountHolder');
      const accNumInput = document.getElementById('bankAccountNumber');
      const ifscInput = document.getElementById('bankIfscCode');
      const bankNameInput = document.getElementById('bankNameSelect');
      const bankAmount = document.getElementById('bankAmountInput');
      const errHolder = document.getElementById('errBankHolder');
      const errAmount = document.getElementById('errBankAmount');

      if (!holderInput.value.trim()) {
        setError(holderInput, errHolder, 'Account holder name is required');
        isValid = false;
      }
      if (!validateAccountNumberField(accNumInput)) isValid = false;
      if (!validateIfscField(ifscInput)) isValid = false;

      amount = parseFloat(bankAmount ? bankAmount.value : 0);
      if (isNaN(amount) || amount <= 0) {
        setError(bankAmount, errAmount, 'Enter a valid amount greater than ₹0');
        isValid = false;
      }

      recipient = holderInput.value.trim();
      const rawAcc = accNumInput.value.trim();
      cardMasked = `A/C •• ${rawAcc.slice(-4)}`;
      bank = bankNameInput ? bankNameInput.value : 'NEFT / IMPS Bank';
    }

    if (!isValid) return null;

    // Build payload matching exact backend contract
    const locationVal = DOM.txLocation ? DOM.txLocation.value : currentUser.primaryLocation;
    const vpnVal = DOM.txVpnOrTor ? DOM.txVpnOrTor.checked : false;

    return {
      amount: amount,
      userId: currentUser.userId,
      userFullName: recipient,
      cardMasked: cardMasked,
      bank: bank,
      currency: currencyCode,
      location: locationVal,
      vpnOrTor: vpnVal,
      deviceHash: `DEV_UPI_WEB_${(navigator.platform || 'CLIENT').replace(/[^a-zA-Z0-9]/g, '_').toUpperCase()}`,
      ipAddress: '127.0.0.1',
      timestamp: Date.now()
    };
  }

  function deriveBankFromVpa(vpa) {
    if (vpa.includes('@okhdfcbank')) return 'HDFC Bank';
    if (vpa.includes('@okaxis')) return 'Axis Bank';
    if (vpa.includes('@oksbi')) return 'State Bank of India';
    if (vpa.includes('@okicici')) return 'ICICI Bank';
    if (vpa.includes('@ybl') || vpa.includes('@ibl')) return 'Yes Bank / PhonePe';
    if (vpa.includes('@paytm')) return 'Paytm Payments Bank';
    if (vpa.includes('@barclays')) return 'Barclays';
    if (vpa.includes('@chase')) return 'Chase';
    return 'UPI Virtual Network';
  }

  function setAnalyzingState(loading) {
    isAnalyzing = loading;
    if (!DOM.btnAnalyze) return;

    if (loading) {
      DOM.btnAnalyze.disabled = true;
      DOM.btnAnalyze.innerHTML = `
        <span class="btn-spinner"></span>
        <span>Evaluating Neural Vectors...</span>
      `;
      DOM.btnAnalyze.classList.add('btn-loading');
    } else {
      DOM.btnAnalyze.disabled = false;
      DOM.btnAnalyze.innerHTML = `
        <span>⚡</span>
        <span>Analyze Transaction</span>
      `;
      DOM.btnAnalyze.classList.remove('btn-loading');
    }
  }

  // ==========================================================
  //  7. FRAUD RESULT RENDERING (EXACT BACKEND DATA & SHAP)
  // ==========================================================
  function renderFraudResult(tx) {
    if (!tx || !tx.fraudEvaluation) return;

    const evalData = tx.fraudEvaluation;
    const riskScore = evalData.riskScore !== undefined ? evalData.riskScore : 0;
    const verdict = evalData.verdict || 'APPROVED';
    const dominantThreat = evalData.dominantThreat || 'Standard Baseline Velocity';
    const attributions = evalData.attributions || [];
    const latency = evalData.latencyMs ? evalData.latencyMs.toFixed(2) : '1.20';

    // Show result container, hide placeholder
    if (DOM.resultEmptyState) DOM.resultEmptyState.classList.add('hidden');
    if (DOM.resultCard) DOM.resultCard.classList.remove('hidden');

    // Risk Score Meter
    if (DOM.resultRiskScore) DOM.resultRiskScore.textContent = `${riskScore}%`;
    if (DOM.resultScoreRing) {
      const circumference = 2 * Math.PI * 45; // r=45
      const offset = circumference - (riskScore / 100) * circumference;
      DOM.resultScoreRing.style.strokeDasharray = `${circumference}`;
      DOM.resultScoreRing.style.strokeDashoffset = `${offset}`;

      // Colorize stroke
      if (verdict === 'AUTO_BLOCKED' || riskScore >= 75) {
        DOM.resultScoreRing.style.stroke = 'var(--color-rose)';
      } else if (verdict === 'CHALLENGE_3DS' || riskScore >= 35) {
        DOM.resultScoreRing.style.stroke = 'var(--color-amber)';
      } else {
        DOM.resultScoreRing.style.stroke = 'var(--color-green)';
      }
    }

    // Verdict Badge & Title
    if (DOM.resultVerdictBadge) {
      DOM.resultVerdictBadge.className = `verdict-badge verdict-${verdict.toLowerCase().replace(/_/g, '-')}`;
      DOM.resultVerdictBadge.textContent = formatVerdictText(verdict);
    }

    if (DOM.resultVerdictTitle) {
      if (verdict === 'AUTO_BLOCKED') {
        DOM.resultVerdictTitle.innerHTML = `<span class="verdict-icon rose">🛑</span> Suspicious Transaction Blocked`;
      } else if (verdict === 'CHALLENGE_3DS') {
        DOM.resultVerdictTitle.innerHTML = `<span class="verdict-icon amber">⚠️</span> 3DS Step-Up Challenge Required`;
      } else {
        DOM.resultVerdictTitle.innerHTML = `<span class="verdict-icon green">✅</span> Autonomous Risk Cleared`;
      }
    }

    if (DOM.resultThreatSignal) DOM.resultThreatSignal.textContent = dominantThreat;
    if (DOM.resultRecipient) DOM.resultRecipient.textContent = tx.userFullName || 'Unknown Merchant';
    if (DOM.resultAmount) DOM.resultAmount.textContent = `${currencySymbol}${parseFloat(tx.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
    if (DOM.resultLatency) DOM.resultLatency.textContent = `${latency} ms`;
    if (DOM.resultTxId) DOM.resultTxId.textContent = tx.transactionId || 'TX-N/A';

    // Render SHAP Feature Attributions
    if (DOM.resultAttributions) {
      DOM.resultAttributions.innerHTML = '';
      if (attributions.length === 0) {
        DOM.resultAttributions.innerHTML = `
          <div class="attribution-empty">No anomalous deviation vectors detected across ensemble.</div>
        `;
      } else {
        attributions.forEach(attr => {
          const sign = attr.impactPoints >= 0 ? '+' : '';
          const isRisk = attr.isRiskIncreasing;
          const badgeClass = isRisk ? 'attr-risk-high' : 'attr-risk-safe';
          const icon = isRisk ? '🔺' : '🛡️';

          const item = document.createElement('div');
          item.className = 'attribution-item';
          item.innerHTML = `
            <div class="attr-header">
              <div class="attr-name">
                <span class="attr-icon">${icon}</span>
                <strong>${escapeHtml(attr.featureName)}</strong>
              </div>
              <span class="attr-score ${badgeClass}">${sign}${attr.impactPoints.toFixed(1)} pts</span>
            </div>
            <p class="attr-desc">${escapeHtml(attr.description)}</p>
          `;
          DOM.resultAttributions.appendChild(item);
        });
      }
    }
  }

  function resetResultView() {
    if (DOM.resultEmptyState) DOM.resultEmptyState.classList.remove('hidden');
    if (DOM.resultCard) DOM.resultCard.classList.add('hidden');
    clearFormErrors();
  }

  // ==========================================================
  //  8. SECURITY LOG & LIVE ALERTS PANEL
  // ==========================================================
  async function refreshSecurityLog(showLoading = false) {
    if (showLoading && DOM.btnRefreshLog) {
      DOM.btnRefreshLog.classList.add('spinning');
    }

    try {
      const res = await SentinelAPI.getRecentTransactions(30);
      if (res && res.data && Array.isArray(res.data)) {
        recentTransactions = res.data;
        renderSecurityLog();
        renderHistoryViewTable();
        renderAlertsViewGrid();
      }
    } catch (err) {
      console.warn('Could not refresh security log:', err.message);
    } finally {
      if (DOM.btnRefreshLog) {
        setTimeout(() => DOM.btnRefreshLog.classList.remove('spinning'), 500);
      }
    }
  }

  function setupSecurityLogEvents() {
    if (DOM.btnRefreshLog) {
      DOM.btnRefreshLog.addEventListener('click', () => {
        refreshSecurityLog(true);
      });
    }

    // Filter Tabs
    DOM.logFilterTabs.forEach(tab => {
      tab.addEventListener('click', () => {
        DOM.logFilterTabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        const filter = tab.getAttribute('data-filter') || 'all';
        renderSecurityLog(filter);
      });
    });

    // Modal close
    if (DOM.btnCloseTxModal) {
      DOM.btnCloseTxModal.addEventListener('click', () => {
        closeModal(DOM.modalTxDetails);
      });
    }
  }

  function renderSecurityLog(filter = 'all') {
    if (!DOM.securityLogList) return;

    let filtered = recentTransactions;
    if (filter === 'blocked') {
      filtered = recentTransactions.filter(tx => tx.fraudEvaluation && tx.fraudEvaluation.verdict === 'AUTO_BLOCKED');
    } else if (filter === 'challenged') {
      filtered = recentTransactions.filter(tx => tx.fraudEvaluation && tx.fraudEvaluation.verdict === 'CHALLENGE_3DS');
    } else if (filter === 'approved') {
      filtered = recentTransactions.filter(tx => tx.fraudEvaluation && tx.fraudEvaluation.verdict === 'APPROVED');
    }

    if (DOM.securityLogCount) {
      DOM.securityLogCount.textContent = `${filtered.length}`;
    }

    if (filtered.length === 0) {
      DOM.securityLogList.innerHTML = `
        <div class="log-empty-state">
          <div class="empty-icon">🛡️</div>
          <h4>No transactions yet</h4>
          <p>Your transaction history and AI risk signals will appear here in real time.</p>
        </div>
      `;
      return;
    }

    DOM.securityLogList.innerHTML = '';

    filtered.slice(0, 15).forEach(tx => {
      const evalData = tx.fraudEvaluation || {};
      const verdict = evalData.verdict || 'APPROVED';
      const threat = evalData.dominantThreat || 'Normal Behavior';
      const riskScore = evalData.riskScore !== undefined ? evalData.riskScore : 0;
      const amt = parseFloat(tx.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
      const curr = tx.currency === 'INR' ? '₹' : (tx.currency === 'USD' ? '$' : '€');
      const timeStr = formatTimestamp(tx.timestamp);

      const card = document.createElement('div');
      card.className = `security-log-card card-verdict-${verdict.toLowerCase().replace(/_/g, '-')}`;
      card.innerHTML = `
        <div class="log-card-top">
          <span class="verdict-tag tag-${verdict.toLowerCase().replace(/_/g, '-')}">
            ${formatVerdictText(verdict)}
          </span>
          <span class="log-risk-score">${riskScore}% Risk</span>
        </div>
        <div class="log-card-main">
          <div class="log-recipient">${escapeHtml(tx.userFullName || 'Direct UPI Transfer')}</div>
          <div class="log-amount">${curr}${amt}</div>
        </div>
        <div class="log-threat-signal">
          <span class="threat-dot"></span>
          <span class="threat-text">${escapeHtml(threat)}</span>
        </div>
        <div class="log-card-footer">
          <span class="log-meta-id">${escapeHtml(tx.transactionId || '')}</span>
          <span class="log-meta-time">${timeStr}</span>
        </div>
      `;

      card.addEventListener('click', () => {
        openTransactionDetailsModal(tx);
      });

      DOM.securityLogList.appendChild(card);
    });
  }

  // ==========================================================
  //  9. FULL HISTORY & ALERTS VIEWS
  // ==========================================================
  function renderHistoryViewTable() {
    const tbody = document.getElementById('historyTableBody');
    if (!tbody) return;

    if (recentTransactions.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">No transactions evaluated yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = '';
    recentTransactions.forEach(tx => {
      const evalData = tx.fraudEvaluation || {};
      const verdict = evalData.verdict || 'APPROVED';
      const riskScore = evalData.riskScore !== undefined ? evalData.riskScore : 0;
      const amt = parseFloat(tx.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
      const curr = tx.currency === 'INR' ? '₹' : '$';

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="font-mono">${escapeHtml(tx.transactionId || '')}</td>
        <td>${escapeHtml(tx.userFullName || 'Unknown')}</td>
        <td><strong>${curr}${amt}</strong></td>
        <td>${escapeHtml(tx.cardMasked || 'UPI')}</td>
        <td>${escapeHtml(tx.location || 'Unknown')}</td>
        <td><span class="verdict-tag tag-${verdict.toLowerCase().replace(/_/g, '-')}">${formatVerdictText(verdict)} (${riskScore}%)</span></td>
        <td><button class="btn btn-xs btn-secondary btn-inspect-tx">Inspect XAI</button></td>
      `;

      tr.querySelector('.btn-inspect-tx').addEventListener('click', (e) => {
        e.stopPropagation();
        openTransactionDetailsModal(tx);
      });

      tbody.appendChild(tr);
    });
  }

  function renderAlertsViewGrid() {
    const alertsContainer = document.getElementById('alertsGridContainer');
    if (!alertsContainer) return;

    const flagged = recentTransactions.filter(tx => {
      const v = tx.fraudEvaluation ? tx.fraudEvaluation.verdict : '';
      return v === 'AUTO_BLOCKED' || v === 'CHALLENGE_3DS';
    });

    if (flagged.length === 0) {
      alertsContainer.innerHTML = `
        <div class="alerts-empty-box">
          <span>🛡️</span>
          <h3>All Clear</h3>
          <p>No high-risk threat vectors or blocked transactions currently detected.</p>
        </div>
      `;
      return;
    }

    alertsContainer.innerHTML = '';
    flagged.forEach(tx => {
      const evalData = tx.fraudEvaluation || {};
      const verdict = evalData.verdict;
      const amt = parseFloat(tx.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
      const curr = tx.currency === 'INR' ? '₹' : '$';

      const alertCard = document.createElement('div');
      alertCard.className = `alert-item-card border-${verdict.toLowerCase().replace(/_/g, '-')}`;
      alertCard.innerHTML = `
        <div class="alert-item-header">
          <span class="verdict-tag tag-${verdict.toLowerCase().replace(/_/g, '-')}">${formatVerdictText(verdict)}</span>
          <span class="alert-score-badge">${evalData.riskScore}% Threat</span>
        </div>
        <h4 class="alert-threat-title">${escapeHtml(evalData.dominantThreat || 'High Anomaly Vector')}</h4>
        <div class="alert-info-row">
          <span>Target / Recipient:</span>
          <strong>${escapeHtml(tx.userFullName || '')}</strong>
        </div>
        <div class="alert-info-row">
          <span>Amount:</span>
          <strong>${curr}${amt}</strong>
        </div>
        <div class="alert-info-row">
          <span>Origin Location:</span>
          <span>${escapeHtml(tx.location || '')}</span>
        </div>
        <div class="alert-info-row">
          <span>Proxy / Tor Mask:</span>
          <span>${tx.vpnOrTor ? '⚠️ Active VPN/Tor Proxy' : 'Direct ISP IP'}</span>
        </div>
        <button class="btn btn-sm btn-primary w-full mt-2 btn-alert-inspect">View Feature Attributions</button>
      `;

      alertCard.querySelector('.btn-alert-inspect').addEventListener('click', () => {
        openTransactionDetailsModal(tx);
      });

      alertsContainer.appendChild(alertCard);
    });
  }

  // ==========================================================
  //  10. USER ACCOUNT SWITCHER
  // ==========================================================
  function setupAccountSwitcher() {
    if (DOM.btnSwitchUser) {
      DOM.btnSwitchUser.addEventListener('click', () => {
        renderUserSwitchList();
        openModal(DOM.modalUserSwitch);
      });
    }

    if (DOM.btnCloseUserModal) {
      DOM.btnCloseUserModal.addEventListener('click', () => {
        closeModal(DOM.modalUserSwitch);
      });
    }

    if (DOM.btnLogout) {
      DOM.btnLogout.addEventListener('click', (e) => {
        e.preventDefault();
        renderUserSwitchList();
        openModal(DOM.modalUserSwitch);
      });
    }
  }

  function renderUserSwitchList() {
    if (!DOM.userListContainer) return;
    DOM.userListContainer.innerHTML = '';

    // Sample fallback accounts if none loaded
    const list = userProfiles.length > 0 ? userProfiles : [
      { userId: 'USR-101', fullName: 'Alexandre V.', primaryLocation: 'London, UK', isBlacklisted: false },
      { userId: 'USR-102', fullName: 'Sarah Jenkins', primaryLocation: 'Austin, US', isBlacklisted: true },
      { userId: 'USR-103', fullName: 'Liam O\'Connor', primaryLocation: 'Dublin, IE', isBlacklisted: false },
      { userId: 'USR-BAD', fullName: 'Botnet Cluster Alpha', primaryLocation: 'Unknown', isBlacklisted: true }
    ];

    list.forEach(u => {
      const isCurrent = u.userId === currentUser.userId;
      const item = document.createElement('div');
      item.className = `user-select-item ${isCurrent ? 'selected-user' : ''}`;
      item.innerHTML = `
        <div class="user-item-avatar">👤</div>
        <div class="user-item-details">
          <div class="user-item-name">
            <strong>${escapeHtml(u.fullName)}</strong>
            ${u.isBlacklisted ? '<span class="badge badge-blacklisted">Blacklisted Identity</span>' : ''}
          </div>
          <div class="user-item-sub">${escapeHtml(u.userId)} &bull; ${escapeHtml(u.primaryLocation || 'Global')}</div>
        </div>
        ${isCurrent ? '<span class="badge badge-accent">Active Session</span>' : '<button class="btn btn-xs btn-secondary">Switch</button>'}
      `;

      item.addEventListener('click', () => {
        currentUser.userId = u.userId;
        currentUser.fullName = u.fullName;
        currentUser.primaryLocation = u.primaryLocation || 'London, UK';
        currentUser.isBlacklisted = !!u.isBlacklisted;
        updateUserSessionUI();
        closeModal(DOM.modalUserSwitch);
        showGlobalNotification(`Switched active account session to ${u.fullName} (${u.userId})`, 'info');
      });

      DOM.userListContainer.appendChild(item);
    });
  }

  // ==========================================================
  //  11. TRANSACTION DETAILS MODAL (XAI INSPECTOR)
  // ==========================================================
  function openTransactionDetailsModal(tx) {
    if (!DOM.modalTxDetails || !DOM.modalTxContent) return;

    const evalData = tx.fraudEvaluation || {};
    const verdict = evalData.verdict || 'APPROVED';
    const attributions = evalData.attributions || [];
    const amt = parseFloat(tx.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
    const curr = tx.currency === 'INR' ? '₹' : '$';

    let attrHtml = '';
    if (attributions.length === 0) {
      attrHtml = `<div class="attribution-empty">No feature attribution anomaly recorded.</div>`;
    } else {
      attributions.forEach(attr => {
        const sign = attr.impactPoints >= 0 ? '+' : '';
        const isRisk = attr.isRiskIncreasing;
        attrHtml += `
          <div class="attribution-item">
            <div class="attr-header">
              <div class="attr-name">
                <span>${isRisk ? '🔺' : '🛡️'}</span>
                <strong>${escapeHtml(attr.featureName)}</strong>
              </div>
              <span class="attr-score ${isRisk ? 'attr-risk-high' : 'attr-risk-safe'}">${sign}${attr.impactPoints.toFixed(1)} pts</span>
            </div>
            <p class="attr-desc">${escapeHtml(attr.description)}</p>
          </div>
        `;
      });
    }

    DOM.modalTxContent.innerHTML = `
      <div class="modal-tx-header">
        <div>
          <div class="modal-tx-id">${escapeHtml(tx.transactionId || 'N/A')}</div>
          <div class="modal-tx-time">${new Date(tx.timestamp || Date.now()).toLocaleString()}</div>
        </div>
        <div class="verdict-badge verdict-${verdict.toLowerCase().replace(/_/g, '-')}">
          ${formatVerdictText(verdict)} (${evalData.riskScore || 0}%)
        </div>
      </div>

      <div class="tx-meta-grid">
        <div class="meta-box">
          <label>Target / Recipient</label>
          <strong>${escapeHtml(tx.userFullName || 'N/A')}</strong>
        </div>
        <div class="meta-box">
          <label>Amount</label>
          <strong class="text-accent">${curr}${amt}</strong>
        </div>
        <div class="meta-box">
          <label>Payment Method</label>
          <span>${escapeHtml(tx.cardMasked || 'UPI')}</span>
        </div>
        <div class="meta-box">
          <label>Issuing / PSP Bank</label>
          <span>${escapeHtml(tx.bank || 'Chase')}</span>
        </div>
        <div class="meta-box">
          <label>Origin Geo</label>
          <span>${escapeHtml(tx.location || 'Unknown')}</span>
        </div>
        <div class="meta-box">
          <label>Proxy / Tor Routing</label>
          <span>${tx.vpnOrTor ? '⚠️ Masked Proxy / Tor Exit' : 'Clean Residential IP'}</span>
        </div>
        <div class="meta-box">
          <label>Device Signature</label>
          <span class="font-mono text-xs">${escapeHtml(tx.deviceHash || 'DEV_UNKNOWN')}</span>
        </div>
        <div class="meta-box">
          <label>Model Processing Latency</label>
          <span>${evalData.latencyMs ? evalData.latencyMs.toFixed(2) : '1.20'} ms</span>
        </div>
      </div>

      <div class="shap-section-title">
        <span>🧠</span> Explainable AI (SHAP) Feature Attributions
      </div>
      <div class="shap-list-container">
        ${attrHtml}
      </div>
    `;

    openModal(DOM.modalTxDetails);
  }

  // ==========================================================
  //  12. UTILITY HELPERS
  // ==========================================================
  function openModal(modalEl) {
    if (modalEl) {
      modalEl.classList.remove('hidden');
      document.body.classList.add('modal-open');
    }
  }

  function closeModal(modalEl) {
    if (modalEl) {
      modalEl.classList.add('hidden');
      document.body.classList.remove('modal-open');
    }
  }

  function formatVerdictText(verdict) {
    if (verdict === 'APPROVED') return 'LOW RISK // APPROVED';
    if (verdict === 'CHALLENGE_3DS') return 'MEDIUM RISK // 3DS CHALLENGE';
    if (verdict === 'AUTO_BLOCKED') return 'HIGH RISK // AUTO BLOCKED';
    return verdict || 'PENDING';
  }

  function formatTimestamp(ts) {
    if (!ts) return 'Just now';
    const diff = Math.floor((Date.now() - ts) / 1000);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return new Date(ts).toLocaleDateString();
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function showGlobalNotification(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `global-toast toast-${type}`;
    toast.innerHTML = `
      <span>${type === 'error' ? '⚠️' : 'ℹ️'}</span>
      <span>${escapeHtml(msg)}</span>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  return {
    init,
    refreshSecurityLog,
    checkBackendHealth
  };
})();

// Bootstrap on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  SentinelTransaction.init();
});
