// Interactive Main-Path Trace Simulator for Afsm
// Interactive Main-Path Trace Simulator for Afsm
(() => {
  const root = document.documentElement;
  const exampleButtons = document.querySelectorAll('[data-open-example]');
  const exampleLab = document.querySelector('#example-lab');
  const traceTitle = document.querySelector('#trace-title');
  const traceSummary = document.querySelector('#trace-summary');
  const traceGuideLink = document.querySelector('#trace-guide-link');
  const traceForm = document.querySelector('#trace-form');
  const tracePhase = document.querySelector('#trace-phase');
  const traceData = document.querySelector('#trace-data');
  const traceList = document.querySelector('#trace-list');
  const traceProgress = document.querySelector('#trace-progress');
  const traceResetButton = document.querySelector('#trace-reset');

  let activeExampleKey = 'draft';
  let activeTraceState = null;
  let traceSequence = 0;

  const getLanguage = () => root.dataset.language || 'en';

  const traceExamples = {
        draft: {
          title: 'Draft',
          summary: {
            en: 'Type a title, save it, and return the repository result as an Event.',
            ko: '제목을 입력하고 저장한 뒤 repository 결과를 Event로 다시 전달합니다.',
          },
          guide: 'https://github.com/kez-lab/afsm/blob/main/docs/getting-started.md',
          initial: {
            phase: 'Editing',
            data: { title: '', errorMessage: null },
          },
        },
        auth: {
          title: 'Auth',
          summary: {
            en: 'Edit the form, submit credentials, and choose the external authentication result.',
            ko: '폼을 직접 편집하고 인증 정보를 제출한 뒤 외부 인증 결과를 선택합니다.',
          },
          guide: 'https://github.com/kez-lab/afsm/blob/main/docs/auth-walkthrough.md',
          initial: {
            phase: 'Editing',
            data: {
              mode: 'Login',
              form: { name: '', email: '', password: '' },
              errorMessage: null,
            },
          },
        },
        checkout: {
          title: 'Checkout',
          summary: {
            en: 'Enter the screen, load a product, pay, and return matching or stale results.',
            ko: '화면 진입, 상품 로딩, 결제, 일치하거나 오래된 결과를 직접 발생시킵니다.',
          },
          guide: 'https://github.com/kez-lab/afsm/blob/main/docs/checkout-walkthrough.md',
          initial: {
            phase: 'Idle',
            data: {
              productId: 42,
              product: null,
              nextPaymentRequestId: 0,
              errorMessage: null,
            },
          },
        },
        'product-editor': {
          title: 'ProductEditor',
          summary: {
            en: 'Edit nested draft Data and drive save, upload, review, and publish results.',
            ko: '중첩된 draft Data를 편집하고 저장, 업로드, 심사, 게시 결과를 직접 진행합니다.',
          },
          guide: 'https://github.com/kez-lab/afsm/blob/main/docs/product-editor-walkthrough.md',
          initial: {
            phase: 'EditingDraft',
            data: {
              draft: {
                form: { title: '', description: '', priceText: '' },
                reviewAttempt: 0,
              },
              errorMessage: null,
            },
          },
        },
      };

      const tr = (en, ko) => getLanguage() === 'ko' ? ko : en;
      const localizedTraceText = value => value[getLanguage()];
      const cloneValue = value => JSON.parse(JSON.stringify(value));
      const valuesEqual = (left, right) => JSON.stringify(left) === JSON.stringify(right);
      const quoted = (value, masked = false) => {
        if (masked && value) return '"' + '•'.repeat(Math.min(value.length, 12)) + '"';
        return JSON.stringify(value);
      };

      const emitTrace = (kind, message) => {
        traceSequence += 1;
        const item = document.createElement('li');
        const index = document.createElement('span');
        const kindLabel = document.createElement('span');
        const detail = document.createElement('span');
        item.className = 'trace-item';
        item.dataset.kind = kind.toLowerCase();
        index.className = 'trace-index';
        kindLabel.className = 'trace-kind';
        detail.className = 'trace-message';
        index.textContent = String(traceSequence).padStart(2, '0');
        kindLabel.textContent = kind;
        detail.textContent = message;
        item.append(index, kindLabel, detail);
        traceList.append(item);
        traceList.scrollTop = traceList.scrollHeight;
      };

      const logData = (path, before, after, masked = false) => {
        if (valuesEqual(before, after)) return;
        emitTrace('Data', path + ': ' + quoted(before, masked) + ' → ' + quoted(after, masked));
      };

      const logPhase = (before, after) => {
        if (before === after) return;
        emitTrace('Phase', before + ' → ' + after);
      };

      const dataForDisplay = () => {
        const data = cloneValue(activeTraceState.data);
        if (activeExampleKey === 'auth' && data.form.password) {
          data.form.password = '•'.repeat(Math.min(data.form.password.length, 12));
        }
        return data;
      };

      const renderLiveState = () => {
        const example = traceExamples[activeExampleKey];
        traceTitle.textContent = example.title;
        traceSummary.textContent = localizedTraceText(example.summary);
        traceGuideLink.href = example.guide;
        tracePhase.textContent = activeTraceState.phase;
        traceData.textContent = JSON.stringify(dataForDisplay(), null, 2);
        traceProgress.textContent = traceSequence + (traceSequence === 1 ? ' record' : ' records');
      };

      const publishTrace = (result, outputs = [], refreshControls = true) => {
        emitTrace('Result', result);
        renderLiveState();
        emitTrace(
          'State',
          tr(
            'State published · phase = ' + activeTraceState.phase,
            'State 게시 · phase = ' + activeTraceState.phase,
          ),
        );
        outputs.forEach(output => emitTrace(output.kind, output.message));
        renderLiveState();
        if (refreshControls) renderInteractionPanel();
      };

      const addField = ({
        field,
        label,
        value = '',
        type = 'text',
        disabled = false,
        options = null,
      }) => {
        const wrapper = document.createElement('div');
        const labelElement = document.createElement('label');
        let control;
        wrapper.className = 'trace-field';
        labelElement.htmlFor = 'trace-field-' + field;
        labelElement.textContent = label;

        if (options) {
          control = document.createElement('select');
          control.className = 'trace-select';
          options.forEach(option => {
            const optionElement = document.createElement('option');
            optionElement.value = option.value;
            optionElement.textContent = option.label;
            control.append(optionElement);
          });
        } else if (type === 'textarea') {
          control = document.createElement('textarea');
          control.className = 'trace-textarea';
        } else {
          control = document.createElement('input');
          control.className = 'trace-input';
          control.type = type;
          if (type === 'password') control.autocomplete = 'current-password';
        }

        control.id = 'trace-field-' + field;
        control.dataset.field = field;
        control.value = value;
        control.disabled = disabled;
        wrapper.append(labelElement, control);
        traceForm.append(wrapper);
        return control;
      };

      const addActions = actions => {
        const wrapper = document.createElement('div');
        wrapper.className = 'trace-form-actions';
        actions.forEach(action => {
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'trace-action' + (action.secondary ? ' trace-action-secondary' : '');
          button.dataset.action = action.id;
          button.textContent = tr(action.en, action.ko);
          wrapper.append(button);
        });
        traceForm.append(wrapper);
      };

      const addFormNote = (en, ko) => {
        const note = document.createElement('p');
        note.className = 'trace-form-note';
        note.textContent = tr(en, ko);
        traceForm.append(note);
      };

      const renderDraftControls = () => {
        const editable = activeTraceState.phase === 'Editing';
        addField({
          field: 'draft-title',
          label: tr('Draft title', 'Draft 제목'),
          value: activeTraceState.data.title,
          disabled: !editable,
        });
        if (editable) {
          addActions([{ id: 'draft-save', en: 'Save draft', ko: 'Draft 저장' }]);
        } else if (activeTraceState.phase === 'Saving') {
          addFormNote(
            'SaveDraft is running. Return the repository result.',
            'SaveDraft가 실행 중입니다. repository 결과를 전달하세요.',
          );
          addActions([
            { id: 'draft-success', en: 'Save succeeded', ko: '저장 성공' },
            { id: 'draft-failure', en: 'Save failed', ko: '저장 실패', secondary: true },
          ]);
        } else {
          addFormNote('The durable Saved State is complete.', '지속되는 Saved State로 완료됐습니다.');
        }
      };

      const renderAuthControls = () => {
        const editing = activeTraceState.phase === 'Editing';
        addField({
          field: 'auth-mode',
          label: tr('Mode', '모드'),
          value: activeTraceState.data.mode,
          disabled: !editing,
          options: [
            { value: 'Login', label: 'Login' },
            { value: 'Register', label: 'Register' },
          ],
        });
        if (activeTraceState.data.mode === 'Register') {
          addField({
            field: 'auth-name',
            label: tr('Name', '이름'),
            value: activeTraceState.data.form.name,
            disabled: !editing,
          });
        }
        addField({
          field: 'auth-email',
          label: tr('Email', '이메일'),
          value: activeTraceState.data.form.email,
          type: 'email',
          disabled: !editing,
        });
        addField({
          field: 'auth-password',
          label: tr('Password', '비밀번호'),
          value: activeTraceState.data.form.password,
          type: 'password',
          disabled: !editing,
        });

        if (editing) {
          addActions([{ id: 'auth-submit', en: 'Submit', ko: '제출' }]);
        } else if (activeTraceState.phase === 'Submitting') {
          addFormNote(
            'The host is executing the authentication Command.',
            'host가 인증 Command를 실행 중입니다.',
          );
          addActions([
            { id: 'auth-success', en: 'Authentication succeeded', ko: '인증 성공' },
            { id: 'auth-failure', en: 'Authentication failed', ko: '인증 실패', secondary: true },
          ]);
        } else {
          addFormNote('Authentication is durable State.', '인증 완료가 지속되는 State입니다.');
        }
      };

      const renderCheckoutControls = () => {
        addFormNote(
          'productId = ' + activeTraceState.data.productId + ' · phase-valid actions only',
          'productId = ' + activeTraceState.data.productId + ' · 현재 phase에서 가능한 동작',
        );
        const phase = activeTraceState.phase;
        if (phase === 'Idle') {
          addActions([{ id: 'checkout-enter', en: 'Enter screen', ko: '화면 진입' }]);
        } else if (phase === 'ProductLoading') {
          addActions([
            { id: 'checkout-loaded', en: 'Product loaded', ko: '상품 로딩 성공' },
            { id: 'checkout-unavailable', en: 'Product unavailable', ko: '상품 없음', secondary: true },
            { id: 'checkout-enter-again', en: 'Enter again', ko: '다시 화면 진입', secondary: true },
          ]);
        } else if (phase === 'ProductReady') {
          addActions([{ id: 'checkout-pay', en: 'Pay', ko: '결제' }]);
        } else if (phase.startsWith('PaymentInProgress')) {
          addActions([
            { id: 'checkout-payment-success', en: 'Payment succeeded', ko: '결제 성공' },
            { id: 'checkout-payment-failure', en: 'Payment failed', ko: '결제 실패', secondary: true },
            { id: 'checkout-stale-result', en: 'Send stale result', ko: '오래된 결과 전송', secondary: true },
          ]);
        } else if (phase === 'PaymentFailed') {
          addActions([{ id: 'checkout-retry', en: 'Retry payment', ko: '결제 재시도' }]);
        } else if (phase.startsWith('Completed')) {
          addActions([{ id: 'checkout-pay-again', en: 'Pay again', ko: '다시 결제', secondary: true }]);
        } else {
          addFormNote('No further action is available.', '더 진행할 수 있는 동작이 없습니다.');
        }
      };

      const renderProductEditorControls = () => {
        const phase = activeTraceState.phase;
        const editable = phase === 'EditingDraft' || phase.startsWith('Rejected');
        addField({
          field: 'editor-title',
          label: tr('Title', '제목'),
          value: activeTraceState.data.draft.form.title,
          disabled: !editable,
        });
        addField({
          field: 'editor-description',
          label: tr('Description', '설명'),
          value: activeTraceState.data.draft.form.description,
          type: 'textarea',
          disabled: !editable,
        });
        addField({
          field: 'editor-price',
          label: tr('Price', '가격'),
          value: activeTraceState.data.draft.form.priceText,
          disabled: !editable,
        });

        if (phase === 'EditingDraft') {
          addActions([
            { id: 'editor-save', en: 'Save draft', ko: 'Draft 저장', secondary: true },
            { id: 'editor-submit', en: 'Submit for review', ko: '심사 제출' },
          ]);
        } else if (phase === 'SavingDraft') {
          addActions([{ id: 'editor-draft-saved', en: 'Draft save completed', ko: 'Draft 저장 완료' }]);
        } else if (phase === 'DraftSaved') {
          addActions([
            { id: 'editor-continue', en: 'Continue editing', ko: '계속 편집', secondary: true },
            { id: 'editor-submit', en: 'Submit for review', ko: '심사 제출' },
          ]);
        } else if (phase === 'ImageUploadInProgress') {
          addActions([
            { id: 'editor-upload-success', en: 'Upload succeeded', ko: '업로드 성공' },
            { id: 'editor-upload-failure', en: 'Upload failed', ko: '업로드 실패', secondary: true },
            { id: 'editor-cancel-upload', en: 'Cancel upload', ko: '업로드 취소', secondary: true },
          ]);
        } else if (phase.startsWith('ReviewSubmissionInProgress')) {
          addActions([
            { id: 'editor-review-approved', en: 'Review approved', ko: '심사 승인' },
            { id: 'editor-review-rejected', en: 'Review rejected', ko: '심사 거절', secondary: true },
          ]);
        } else if (phase.startsWith('Rejected')) {
          addActions([
            { id: 'editor-continue', en: 'Continue editing', ko: '계속 편집', secondary: true },
            { id: 'editor-resubmit', en: 'Resubmit for review', ko: '심사 재제출' },
          ]);
        } else if (phase === 'Approved') {
          addActions([
            { id: 'editor-continue', en: 'Continue editing', ko: '계속 편집', secondary: true },
            { id: 'editor-publish', en: 'Publish', ko: '게시' },
          ]);
        } else if (phase === 'PublishInProgress') {
          addActions([
            { id: 'editor-publish-success', en: 'Publish succeeded', ko: '게시 성공' },
            { id: 'editor-publish-failure', en: 'Publish failed', ko: '게시 실패', secondary: true },
          ]);
        } else {
          addFormNote('The product is published.', '상품 게시가 완료됐습니다.');
        }
      };

      function renderInteractionPanel() {
        traceForm.replaceChildren();
        if (activeExampleKey === 'draft') renderDraftControls();
        if (activeExampleKey === 'auth') renderAuthControls();
        if (activeExampleKey === 'checkout') renderCheckoutControls();
        if (activeExampleKey === 'product-editor') renderProductEditorControls();
      }

      const resetExampleLab = () => {
        const example = traceExamples[activeExampleKey];
        activeTraceState = cloneValue(example.initial);
        traceSequence = 0;
        traceList.replaceChildren();
        renderLiveState();
        renderInteractionPanel();
        emitTrace(
          'State',
          tr(
            'Initial State · phase = ' + activeTraceState.phase,
            '초기 State · phase = ' + activeTraceState.phase,
          ),
        );
        renderLiveState();
      };

      const selectExample = (key, shouldScroll = true) => {
        activeExampleKey = key;
        exampleLab.hidden = false;
        exampleButtons.forEach(button => {
          const selected = button.dataset.openExample === key;
          button.classList.toggle('is-selected', selected);
          button.setAttribute('aria-pressed', String(selected));
        });
        resetExampleLab();
        if (shouldScroll) {
          const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
          exampleLab.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' });
        }
      };

      const updateField = (path, eventName, value, masked = false) => {
        let target = activeTraceState.data;
        for (let index = 0; index < path.length - 1; index += 1) {
          target = target[path[index]];
        }
        const key = path[path.length - 1];
        const before = target[key];
        if (before === value) return;
        emitTrace('Event', eventName + '(' + quoted(value, masked) + ')');
        target[key] = value;
        logData(path.join('.'), before, value, masked);
        if (activeTraceState.data.errorMessage !== null) {
          const previousError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = null;
          logData('errorMessage', previousError, null);
        }
        publishTrace(tr('Handled · phase unchanged', 'Handled · phase 유지'), [], false);
      };

      const handleTraceInput = target => {
        const field = target.dataset.field;
        const value = target.value;
        if (field === 'draft-title' && activeTraceState.phase === 'Editing') {
          updateField(['title'], 'TitleChanged', value);
        }
        if (field === 'auth-name' && activeTraceState.phase === 'Editing') {
          updateField(['form', 'name'], 'NameChanged', value);
        }
        if (field === 'auth-email' && activeTraceState.phase === 'Editing') {
          updateField(['form', 'email'], 'EmailChanged', value);
        }
        if (field === 'auth-password' && activeTraceState.phase === 'Editing') {
          updateField(['form', 'password'], 'PasswordChanged', value, true);
        }
        if (field === 'editor-title' && (activeTraceState.phase === 'EditingDraft' || activeTraceState.phase.startsWith('Rejected'))) {
          updateField(['draft', 'form', 'title'], 'TitleChanged', value);
        }
        if (field === 'editor-description' && (activeTraceState.phase === 'EditingDraft' || activeTraceState.phase.startsWith('Rejected'))) {
          updateField(['draft', 'form', 'description'], 'DescriptionChanged', value);
        }
        if (field === 'editor-price' && (activeTraceState.phase === 'EditingDraft' || activeTraceState.phase.startsWith('Rejected'))) {
          updateField(['draft', 'form', 'priceText'], 'PriceChanged', value);
        }
      };

      const handleAuthModeChange = value => {
        if (activeTraceState.phase !== 'Editing' || activeTraceState.data.mode === value) return;
        const before = activeTraceState.data.mode;
        emitTrace('Event', 'ModeChanged(' + value + ')');
        activeTraceState.data.mode = value;
        logData('mode', before, value);
        publishTrace(tr('Handled · form mode changed', 'Handled · form mode 변경'));
      };

      const handleDraftAction = action => {
        if (action === 'draft-save' && activeTraceState.phase === 'Editing') {
          emitTrace('Event', 'SaveClicked');
          if (!activeTraceState.data.title.trim()) {
            emitTrace('Guard', 'case "missing title" → true');
            const before = activeTraceState.data.errorMessage;
            activeTraceState.data.errorMessage = 'Title is required.';
            logData('errorMessage', before, activeTraceState.data.errorMessage);
            publishTrace(tr('Handled · phase remains Editing', 'Handled · Editing phase 유지'));
            return;
          }
          emitTrace('Guard', 'case "valid title" → true');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Saving';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(
            tr('Transitioned · save work requested', 'Transitioned · 저장 작업 요청'),
            [{ kind: 'Command', message: 'SaveDraft(title = ' + quoted(activeTraceState.data.title) + ')' }],
          );
        }
        if (action === 'draft-success' && activeTraceState.phase === 'Saving') {
          emitTrace('Event', 'DraftSaveCompleted');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Saved';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · durable completion', 'Transitioned · 지속되는 완료'));
        }
        if (action === 'draft-failure' && activeTraceState.phase === 'Saving') {
          emitTrace('Event', 'DraftSaveFailed("Draft save failed.")');
          const beforeError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Draft save failed.';
          logData('errorMessage', beforeError, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Editing';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · return to editing', 'Transitioned · 편집으로 복귀'));
        }
      };

      const authSubmitError = () => {
        const form = activeTraceState.data.form;
        if (!form.email.trim()) return 'Email is required.';
        if (form.password.length < 6) return 'Password must be at least 6 characters.';
        if (activeTraceState.data.mode === 'Register' && !form.name.trim()) return 'Name is required.';
        return null;
      };

      const normalizeAuthForm = () => {
        const form = activeTraceState.data.form;
        const beforeName = form.name;
        const beforeEmail = form.email;
        form.name = form.name.trim();
        form.email = form.email.trim();
        logData('form.name', beforeName, form.name);
        logData('form.email', beforeEmail, form.email);
      };

      const handleAuthAction = action => {
        if (action === 'auth-submit' && activeTraceState.phase === 'Editing') {
          emitTrace('Event', 'SubmitClicked');
          const error = authSubmitError();
          if (error) {
            emitTrace('Guard', 'case "invalid form" → true');
            normalizeAuthForm();
            const before = activeTraceState.data.errorMessage;
            activeTraceState.data.errorMessage = error;
            logData('errorMessage', before, error);
            publishTrace(tr('Handled · validation error stays in Data', 'Handled · validation error를 Data에 저장'));
            return;
          }
          emitTrace('Guard', 'case "' + activeTraceState.data.mode.toLowerCase() + ' form" → true');
          normalizeAuthForm();
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Submitting';
          logPhase(beforePhase, activeTraceState.phase);
          const form = activeTraceState.data.form;
          const command = activeTraceState.data.mode === 'Login'
            ? 'Login(email = ' + quoted(form.email) + ', password = "••••••")'
            : 'Register(name = ' + quoted(form.name) + ', email = ' + quoted(form.email) + ', password = "••••••")';
          publishTrace(
            tr('Transitioned · authentication requested', 'Transitioned · 인증 요청'),
            [{ kind: 'Command', message: command }],
          );
        }
        if (action === 'auth-success' && activeTraceState.phase === 'Submitting') {
          emitTrace('Event', 'AuthSucceeded(session)');
          const email = activeTraceState.data.form.email;
          const beforeData = cloneValue(activeTraceState.data);
          activeTraceState.data = cloneValue(traceExamples.auth.initial.data);
          emitTrace('Data', 'AuthData(form) → AuthData()');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Authenticated(email=' + email + ')';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · authenticated State', 'Transitioned · 인증 State'));
        }
        if (action === 'auth-failure' && activeTraceState.phase === 'Submitting') {
          emitTrace('Event', 'AuthFailed("Invalid credentials.")');
          const before = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Invalid credentials.';
          logData('errorMessage', before, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Editing';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · retry is available', 'Transitioned · 재시도 가능'));
        }
      };

      const checkoutRequestId = () => Number((activeTraceState.phase.match(/requestId=(\d+)/) || [])[1] || 0);

      const startPayment = eventName => {
        emitTrace('Event', eventName);
        emitTrace('Guard', 'case "product loaded" → true');
        const beforeId = activeTraceState.data.nextPaymentRequestId;
        activeTraceState.data.nextPaymentRequestId += 1;
        activeTraceState.data.errorMessage = null;
        logData('nextPaymentRequestId', beforeId, activeTraceState.data.nextPaymentRequestId);
        const beforePhase = activeTraceState.phase;
        activeTraceState.phase = 'PaymentInProgress(requestId=' + activeTraceState.data.nextPaymentRequestId + ')';
        logPhase(beforePhase, activeTraceState.phase);
        publishTrace(
          tr('Transitioned · payment requested', 'Transitioned · 결제 요청'),
          [{
            kind: 'Command',
            message: 'SubmitPayment(requestId = ' + activeTraceState.data.nextPaymentRequestId + ', productId = 42)',
          }],
        );
      };

      const handleCheckoutAction = action => {
        if (action === 'checkout-enter' && activeTraceState.phase === 'Idle') {
          emitTrace('Event', 'ScreenEntered');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'ProductLoading';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(
            tr('Transitioned · product loading entered', 'Transitioned · 상품 로딩 진입'),
            [{ kind: 'Command', message: 'LoadProduct(productId = 42)' }],
          );
        }
        if (action === 'checkout-enter-again' && activeTraceState.phase === 'ProductLoading') {
          emitTrace('Event', 'ScreenEntered');
          publishTrace(tr('Ignored · product load already in flight', 'Ignored · 상품 로딩 진행 중'));
        }
        if (action === 'checkout-loaded' && activeTraceState.phase === 'ProductLoading') {
          emitTrace('Event', 'ProductLoaded(product = Compose Backpack)');
          const before = activeTraceState.data.product;
          activeTraceState.data.product = { id: 42, title: 'Compose Backpack', priceCents: 12900 };
          logData('product', before, activeTraceState.data.product);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'ProductReady';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · product ready', 'Transitioned · 상품 준비 완료'));
        }
        if (action === 'checkout-unavailable' && activeTraceState.phase === 'ProductLoading') {
          emitTrace('Event', 'ProductUnavailable');
          const beforeError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Product is no longer available.';
          logData('errorMessage', beforeError, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'ProductUnavailable';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · product unavailable', 'Transitioned · 상품 없음'));
        }
        if (action === 'checkout-pay' && activeTraceState.phase === 'ProductReady') {
          startPayment('PayClicked');
        }
        if (action === 'checkout-retry' && activeTraceState.phase === 'PaymentFailed') {
          startPayment('RetryClicked');
        }
        if (action === 'checkout-stale-result' && activeTraceState.phase.startsWith('PaymentInProgress')) {
          emitTrace('Event', 'PaymentSucceeded(requestId = 0, orderId = 8999)');
          emitTrace('Guard', 'case "matching request" → false');
          publishTrace(tr('Ignored · stale payment success result', 'Ignored · 오래된 결제 성공 결과'));
        }
        if (action === 'checkout-payment-success' && activeTraceState.phase.startsWith('PaymentInProgress')) {
          const requestId = checkoutRequestId();
          emitTrace('Event', 'PaymentSucceeded(requestId = ' + requestId + ', orderId = 9001)');
          emitTrace('Guard', 'case "matching request" → true');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Completed(orderId=9001)';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · durable completion', 'Transitioned · 지속되는 완료'));
        }
        if (action === 'checkout-payment-failure' && activeTraceState.phase.startsWith('PaymentInProgress')) {
          const requestId = checkoutRequestId();
          emitTrace('Event', 'PaymentFailed(requestId = ' + requestId + ', message = "Card declined.")');
          emitTrace('Guard', 'case "matching request" → true');
          const beforeError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Card declined.';
          logData('errorMessage', beforeError, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'PaymentFailed';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · retry is available', 'Transitioned · 재시도 가능'));
        }
        if (action === 'checkout-pay-again' && activeTraceState.phase.startsWith('Completed')) {
          emitTrace('Event', 'PayClicked');
          publishTrace(tr('Ignored · checkout already complete', 'Ignored · 결제 이미 완료'));
        }
      };

      const editorValidationError = () => {
        const form = activeTraceState.data.draft.form;
        const price = Number(form.priceText);
        if (!form.title.trim()) return 'Title is required.';
        if (form.description.trim().length < 10) return 'Description must be at least 10 characters.';
        if (!Number.isFinite(price) || price <= 0) return 'Enter a valid price.';
        return null;
      };

      const normalizeEditorForm = () => {
        const form = activeTraceState.data.draft.form;
        ['title', 'description', 'priceText'].forEach(key => {
          const before = form[key];
          form[key] = form[key].trim();
          logData('draft.form.' + key, before, form[key]);
        });
      };

      const submitEditor = eventName => {
        emitTrace('Event', eventName);
        const error = editorValidationError();
        if (error) {
          emitTrace('Guard', 'case "invalid draft" → true');
          const before = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = error;
          logData('errorMessage', before, error);
          publishTrace(tr('Handled · validation error stays in Data', 'Handled · validation error를 Data에 저장'));
          return;
        }
        emitTrace('Guard', 'case "valid draft" → true');
        normalizeEditorForm();
        const beforePhase = activeTraceState.phase;
        activeTraceState.phase = 'ImageUploadInProgress';
        logPhase(beforePhase, activeTraceState.phase);
        publishTrace(
          tr('Transitioned · upload phase owns work', 'Transitioned · upload phase가 작업 소유'),
          [{ kind: 'Invoke', message: 'StartImageUpload(draft) · cancellable on phase exit' }],
        );
      };

      const handleEditorAction = action => {
        if (action === 'editor-save' && activeTraceState.phase === 'EditingDraft') {
          emitTrace('Event', 'SaveDraftClicked');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'SavingDraft';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(
            tr('Transitioned · draft save requested', 'Transitioned · draft 저장 요청'),
            [{ kind: 'Command', message: 'SaveDraft(draft)' }],
          );
        }
        if (action === 'editor-draft-saved' && activeTraceState.phase === 'SavingDraft') {
          emitTrace('Event', 'DraftSaveCompleted');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'DraftSaved';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · draft saved', 'Transitioned · draft 저장 완료'));
        }
        if (action === 'editor-continue' && (
          activeTraceState.phase === 'DraftSaved' ||
          activeTraceState.phase.startsWith('Rejected') ||
          activeTraceState.phase === 'Approved'
        )) {
          emitTrace('Event', 'ContinueEditingClicked');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'EditingDraft';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · editing resumed', 'Transitioned · 편집 재개'));
        }
        if (action === 'editor-submit' && (
          activeTraceState.phase === 'EditingDraft' ||
          activeTraceState.phase === 'DraftSaved'
        )) {
          submitEditor('SubmitClicked');
        }
        if (action === 'editor-resubmit' && activeTraceState.phase.startsWith('Rejected')) {
          submitEditor('ResubmitClicked');
        }
        if (action === 'editor-upload-success' && activeTraceState.phase === 'ImageUploadInProgress') {
          emitTrace('Event', 'ImageUploadSucceeded(uploadToken = "upload-1")');
          const beforeAttempt = activeTraceState.data.draft.reviewAttempt;
          activeTraceState.data.draft.reviewAttempt += 1;
          logData('draft.reviewAttempt', beforeAttempt, activeTraceState.data.draft.reviewAttempt);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'ReviewSubmissionInProgress(upload-1)';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(
            tr('Transitioned · review submission entered', 'Transitioned · 심사 제출 진입'),
            [{ kind: 'Command', message: 'StartReviewSubmission(draft, uploadToken = "upload-1")' }],
          );
        }
        if (action === 'editor-upload-failure' && activeTraceState.phase === 'ImageUploadInProgress') {
          emitTrace('Event', 'ImageUploadFailed("Image upload failed.")');
          const beforeError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Image upload failed.';
          logData('errorMessage', beforeError, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'EditingDraft';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · return to editing', 'Transitioned · 편집으로 복귀'));
        }
        if (action === 'editor-cancel-upload' && activeTraceState.phase === 'ImageUploadInProgress') {
          emitTrace('Event', 'CancelUploadClicked');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'EditingDraft';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · phase-owned upload cancelled', 'Transitioned · phase 소유 업로드 취소'));
        }
        if (action === 'editor-review-approved' && activeTraceState.phase.startsWith('ReviewSubmissionInProgress')) {
          emitTrace('Event', 'ReviewApproved');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Approved';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · publish is available', 'Transitioned · 게시 가능'));
        }
        if (action === 'editor-review-rejected' && activeTraceState.phase.startsWith('ReviewSubmissionInProgress')) {
          emitTrace('Event', 'ReviewRejected("Add more detail.")');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Rejected(reason=Add more detail.)';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · draft can be edited and resubmitted', 'Transitioned · 수정 후 재제출 가능'));
        }
        if (action === 'editor-publish' && activeTraceState.phase === 'Approved') {
          emitTrace('Event', 'PublishClicked');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'PublishInProgress';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(
            tr('Transitioned · publish requested', 'Transitioned · 게시 요청'),
            [{ kind: 'Command', message: 'StartProductPublish(draft)' }],
          );
        }
        if (action === 'editor-publish-success' && activeTraceState.phase === 'PublishInProgress') {
          emitTrace('Event', 'PublishSucceeded(productId = 501)');
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Published(productId=501, title=' + activeTraceState.data.draft.form.title + ')';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · durable published State', 'Transitioned · 지속되는 게시 State'));
        }
        if (action === 'editor-publish-failure' && activeTraceState.phase === 'PublishInProgress') {
          emitTrace('Event', 'PublishFailed("Publishing failed.")');
          const beforeError = activeTraceState.data.errorMessage;
          activeTraceState.data.errorMessage = 'Publishing failed.';
          logData('errorMessage', beforeError, activeTraceState.data.errorMessage);
          const beforePhase = activeTraceState.phase;
          activeTraceState.phase = 'Approved';
          logPhase(beforePhase, activeTraceState.phase);
          publishTrace(tr('Transitioned · publish can be retried', 'Transitioned · 게시 재시도 가능'));
        }
      };

      const handleTraceAction = action => {
        if (action.startsWith('draft-')) handleDraftAction(action);
        if (action.startsWith('auth-')) handleAuthAction(action);
        if (action.startsWith('checkout-')) handleCheckoutAction(action);
        if (action.startsWith('editor-')) handleEditorAction(action);
      };

      exampleButtons.forEach(button => {
        button.addEventListener('click', () => selectExample(button.dataset.openExample));
      });
      traceForm.addEventListener('input', event => handleTraceInput(event.target));
      traceForm.addEventListener('change', event => {
        if (event.target.dataset.field === 'auth-mode') handleAuthModeChange(event.target.value);
      });
      traceForm.addEventListener('click', event => {
        const button = event.target.closest('[data-action]');
        if (button) handleTraceAction(button.dataset.action);
      });
      traceResetButton.addEventListener('click', resetExampleLab);

  window.afsmTraceLab = {
    selectExample,
    resetExampleLab,
  };
})();
