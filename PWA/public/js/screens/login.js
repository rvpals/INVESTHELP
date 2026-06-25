import { auth, users } from '../api.js';

/**
 * Render the login screen (or first-run setup) into the given container.
 * Calls onSuccess() when the user authenticates successfully.
 */
export async function renderLogin(container, onSuccess) {
  const setupNeeded = await auth.setupNeeded();
  if (setupNeeded) {
    renderSetup(container, onSuccess);
  } else {
    renderLoginForm(container, onSuccess);
  }
}

// ---------------------------------------------------------------------------
// Login form — shown when at least one user exists
// ---------------------------------------------------------------------------

function renderLoginForm(container, onSuccess) {
  container.innerHTML = `
    <div style="
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--background, #fffbfe);
      padding: 24px;
    ">
      <div class="card" style="width:100%;max-width:380px;padding:32px 28px">
        <div style="text-align:center;margin-bottom:28px">
          <div style="font-size:40px;margin-bottom:8px">&#128200;</div>
          <div style="font-size:22px;font-weight:700;color:var(--text-primary)">InvestHelp</div>
          <div class="text-sm text-muted mt-4">Sign in to your account</div>
        </div>

        <div id="login-error" class="hidden" style="
          background: var(--error-container, #ffdad6);
          color: var(--on-error-container, #410002);
          border-radius: 8px;
          padding: 10px 14px;
          font-size: 13px;
          margin-bottom: 16px;
        "></div>

        <form id="login-form" autocomplete="on">
          <div class="form-group" style="margin-bottom:16px">
            <label class="label" for="login-username">Username</label>
            <input
              class="input"
              type="text"
              id="login-username"
              name="username"
              autocomplete="username"
              placeholder="Enter username"
              autofocus
              required
            >
          </div>
          <div class="form-group" style="margin-bottom:24px">
            <label class="label" for="login-password">Password</label>
            <input
              class="input"
              type="password"
              id="login-password"
              name="password"
              autocomplete="current-password"
              placeholder="Enter password"
              required
            >
          </div>
          <button class="btn btn-primary w-full" type="submit" id="login-btn">
            Sign In
          </button>
        </form>
      </div>
    </div>
  `;

  const form    = document.getElementById('login-form');
  const errorEl = document.getElementById('login-error');
  const btn     = document.getElementById('login-btn');

  function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.remove('hidden');
  }

  function clearError() {
    errorEl.textContent = '';
    errorEl.classList.add('hidden');
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearError();

    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    if (!username || !password) {
      showError('Please enter your username and password.');
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Signing in…';

    try {
      await auth.login(username, password);
      onSuccess();
    } catch (err) {
      showError(err.message || 'Login failed. Please try again.');
      btn.disabled = false;
      btn.textContent = 'Sign In';
    }
  });
}

// ---------------------------------------------------------------------------
// First-run setup form — shown when no users exist yet
// ---------------------------------------------------------------------------

function renderSetup(container, onSuccess) {
  container.innerHTML = `
    <div style="
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--background, #fffbfe);
      padding: 24px;
    ">
      <div class="card" style="width:100%;max-width:400px;padding:32px 28px">
        <div style="text-align:center;margin-bottom:24px">
          <div style="font-size:40px;margin-bottom:8px">&#128200;</div>
          <div style="font-size:22px;font-weight:700;color:var(--text-primary)">Welcome to InvestHelp</div>
          <div class="text-sm text-muted mt-4">Create your administrator account to get started.</div>
        </div>

        <div style="
          background: var(--primary-container, #e8def8);
          color: var(--on-primary-container, #21005d);
          border-radius: 8px;
          padding: 10px 14px;
          font-size: 13px;
          margin-bottom: 20px;
        ">
          &#128274; No accounts exist yet. Set up your first account below.
        </div>

        <div id="setup-error" class="hidden" style="
          background: var(--error-container, #ffdad6);
          color: var(--on-error-container, #410002);
          border-radius: 8px;
          padding: 10px 14px;
          font-size: 13px;
          margin-bottom: 16px;
        "></div>

        <form id="setup-form" autocomplete="off">
          <div class="form-group" style="margin-bottom:16px">
            <label class="label" for="setup-username">Username</label>
            <input
              class="input"
              type="text"
              id="setup-username"
              name="username"
              autocomplete="off"
              placeholder="Choose a username (min 2 chars)"
              autofocus
              required
            >
          </div>
          <div class="form-group" style="margin-bottom:16px">
            <label class="label" for="setup-password">Password</label>
            <input
              class="input"
              type="password"
              id="setup-password"
              name="new-password"
              autocomplete="new-password"
              placeholder="Choose a password (min 4 chars)"
              required
            >
          </div>
          <div class="form-group" style="margin-bottom:24px">
            <label class="label" for="setup-password2">Confirm Password</label>
            <input
              class="input"
              type="password"
              id="setup-password2"
              name="confirm-password"
              autocomplete="new-password"
              placeholder="Confirm your password"
              required
            >
          </div>
          <button class="btn btn-primary w-full" type="submit" id="setup-btn">
            Create Account &amp; Sign In
          </button>
        </form>
      </div>
    </div>
  `;

  const form    = document.getElementById('setup-form');
  const errorEl = document.getElementById('setup-error');
  const btn     = document.getElementById('setup-btn');

  function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.remove('hidden');
  }

  function clearError() {
    errorEl.textContent = '';
    errorEl.classList.add('hidden');
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearError();

    const username  = document.getElementById('setup-username').value.trim();
    const password  = document.getElementById('setup-password').value;
    const password2 = document.getElementById('setup-password2').value;

    if (!username || !password || !password2) {
      showError('All fields are required.');
      return;
    }
    if (username.length < 2) {
      showError('Username must be at least 2 characters.');
      return;
    }
    if (password.length < 4) {
      showError('Password must be at least 4 characters.');
      return;
    }
    if (password !== password2) {
      showError('Passwords do not match.');
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Creating account…';

    try {
      await users.create(username, password);
      // Account created — now log in automatically
      await auth.login(username, password);
      onSuccess();
    } catch (err) {
      showError(err.message || 'Account creation failed. Please try again.');
      btn.disabled = false;
      btn.textContent = 'Create Account & Sign In';
    }
  });
}
