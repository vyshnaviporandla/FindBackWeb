'use strict';

const API_URL = '/api/items';
const itemsGrid = document.getElementById('itemsGrid');
const itemsMessage = document.getElementById('itemsMessage');
const resultsSummary = document.getElementById('resultsSummary');
const totalReports = document.getElementById('totalReports');
const openReports = document.getElementById('openReports');
const resolvedReports = document.getElementById('resolvedReports');
const searchInput = document.getElementById('searchInput');
const typeFilter = document.getElementById('typeFilter');
const categoryFilter = document.getElementById('categoryFilter');
const locationFilter = document.getElementById('locationFilter');
const refreshButton = document.getElementById('refreshButton');
const clearFiltersButton = document.getElementById('clearFiltersButton');
const reportForm = document.getElementById('reportForm');
const formMessage = document.getElementById('formMessage');
const submitButton = document.getElementById('submitButton');
const cancelEditButton = document.getElementById('cancelEditButton');
const itemPhoto = document.getElementById('itemPhoto');
const photoPreviewContainer = document.getElementById('photoPreviewContainer');
const photoPreview = document.getElementById('photoPreview');
const photoFileName = document.getElementById('photoFileName');
const removePhotoButton = document.getElementById('removePhotoButton');
const reportFormTitle = document.getElementById('reportFormTitle');
const authPanel = document.getElementById('authPanel');
const authForm = document.getElementById('authForm');
const authPanelTitle = document.getElementById('authPanelTitle');
const authPanelDescription = document.getElementById('authPanelDescription');
const authNameField = document.getElementById('authNameField');
const authDisplayName = document.getElementById('authDisplayName');
const authEmail = document.getElementById('authEmail');
const authPassword = document.getElementById('authPassword');
const authSubmitButton = document.getElementById('authSubmitButton');
const authModeToggle = document.getElementById('authModeToggle');
const authCloseButton = document.getElementById('authCloseButton');
const authMessage = document.getElementById('authMessage');
const accountButton = document.getElementById('accountButton');
const logoutButton = document.getElementById('logoutButton');
const myReportsLink = document.getElementById('myReportsLink');

const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const ALLOWED_PHOTO_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const DEFAULT_CATEGORIES = [
    'Electronics', 'Books & Stationery', 'Bags & Accessories', 'Clothing',
    'Keys & Cards', 'Bottle', 'Documents', 'Jewellery', 'Other'
];

let allItems = [];
let selectedPhoto = null;
let photoPreviewUrl = null; // Only a temporary browser-created URL; never an existing server path.
let editingItemId = null;
let existingPhotoPath = null;
let removeExistingPhoto = false;
let currentUser = null;
let registerMode = false;
let showingMyReports = false;

function escapeHTML(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character]);
}

function safeImagePath(path) {
    if (typeof path !== 'string') return '';
    return path.startsWith('/uploads/') && !path.includes('..') ? path : '';
}

function itemIcon(category, reportType) {
    const text = String(category || '').toLowerCase();
    if (text.includes('electronic')) return 'Electronics';
    if (text.includes('book') || text.includes('stationery')) return 'Books';
    if (text.includes('bag')) return 'Bags';
    if (text.includes('key') || text.includes('card')) return 'Keys';
    if (text.includes('bottle')) return 'Bottle';
    if (text.includes('document')) return 'Document';
    if (text.includes('cloth')) return 'Clothing';
    return String(reportType).toUpperCase() === 'FOUND' ? '✓' : '⌕';
}

function formatDate(value) {
    if (!value) return 'Date not provided';
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) return escapeHTML(value);
    return new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(parsed);
}

function normalise(value) {
    return String(value ?? '').trim().toLowerCase();
}

function populateCategories() {
    const currentValue = categoryFilter.value || 'ALL';
    const names = new Set(DEFAULT_CATEGORIES);
    allItems.forEach(item => { if (item.category) names.add(String(item.category).trim()); });
    const sortedNames = [...names].sort((a, b) => a.localeCompare(b));
    categoryFilter.innerHTML = '<option value="ALL">All categories</option>' +
        sortedNames.map(name => `<option value="${escapeHTML(name)}">${escapeHTML(name)}</option>`).join('');
    if ([...categoryFilter.options].some(option => option.value === currentValue)) categoryFilter.value = currentValue;
}

function updateStats() {
    totalReports.textContent = String(allItems.length);
    openReports.textContent = String(allItems.filter(item => normalise(item.status) === 'open').length);
    resolvedReports.textContent = String(allItems.filter(item => normalise(item.status) === 'resolved').length);
}

function getFilteredItems() {
    const query = normalise(searchInput.value);
    const selectedType = typeFilter.value;
    const selectedCategory = categoryFilter.value;
    const locationQuery = normalise(locationFilter.value);
    return allItems.filter(item => {
        const matchesText = !query || [item.itemName, item.description, item.category, item.location]
            .some(value => normalise(value).includes(query));
        const matchesType = selectedType === 'ALL' || normalise(item.reportType) === normalise(selectedType);
        const matchesCategory = selectedCategory === 'ALL' || normalise(item.category) === normalise(selectedCategory);
        const matchesLocation = !locationQuery || normalise(item.location).includes(locationQuery);
        const matchesOwnership = !showingMyReports || item.ownedByCurrentUser === true;
        return matchesText && matchesType && matchesCategory && matchesLocation && matchesOwnership;
    }).sort((a, b) => {
        const aOpen = normalise(a.status) === 'open' ? 0 : 1;
        const bOpen = normalise(b.status) === 'open' ? 0 : 1;
        return aOpen !== bOpen ? aOpen - bOpen : Number(b.id || 0) - Number(a.id || 0);
    });
}

function renderItems() {
    const items = getFilteredItems();
    resultsSummary.textContent = `${items.length} ${items.length === 1 ? 'report' : 'reports'} shown`;
    if (items.length === 0) {
        itemsGrid.innerHTML = showingMyReports
            ? '<div class="empty-state"><div class="empty-icon" aria-hidden="true">⌕</div><h3>No reports in My Reports yet</h3><p>Reports you submit while signed in will appear here. Older reports may need to be assigned to your account first.</p></div>'
            : '<div class="empty-state"><div class="empty-icon" aria-hidden="true">⌕</div><h3>No reports found</h3><p>Try changing your search or filters, or submit a new report.</p></div>';
        return;
    }

    itemsGrid.innerHTML = items.map(item => {
        const reportType = normalise(item.reportType) === 'found' ? 'FOUND' : 'LOST';
        const isOpen = normalise(item.status) === 'open';
        const isResolved = normalise(item.status) === 'resolved';
        const imagePath = safeImagePath(item.imagePath);
        const safeName = escapeHTML(item.itemName || 'Unnamed item');
        const description = String(item.description || '').trim();
        const truncatedDescription = description.length > 180 ? `${description.slice(0, 177)}…` : description;
        const imageMarkup = imagePath
            ? `<img class="card-image" src="${escapeHTML(imagePath)}" alt="Photo of ${safeName}" loading="lazy">`
            : `<span class="image-placeholder" aria-hidden="true">${escapeHTML(itemIcon(item.category, reportType))}</span>`;
        const date = item.itemDate ? formatDate(item.itemDate) : 'Date not provided';
        const statusLabel = isResolved ? 'RESOLVED' : (isOpen ? 'OPEN' : escapeHTML(item.status || 'CLOSED').toUpperCase());
        const isOwner = item.ownedByCurrentUser === true;
        const matchAndResolveButtons = isOpen ? `
            <button class="match-button" type="button" data-action="matches" data-id="${Number(item.id)}" aria-expanded="false">* Check matches</button>
            ${isOwner ? `<button class="resolve-button" type="button" data-action="resolve" data-id="${Number(item.id)}">Mark resolved</button>` : ''}` : '';
        const ownerEditButton = isOwner
            ? `<buttonEdit report</button>` : '';
        const actions = `
            <div class="card-actions">
                ${ownerEditButton}
                ${matchAndResolveButtons}
            </div>
            ${isOpen ? `<div class="match-results" id="matches-${Number(item.id)}" aria-live="polite"></div>` : ''}`;

        return `<article class="item-card" data-item-id="${Number(item.id)}">
            <div class="card-image-wrap">${imageMarkup}
                <span class="card-type-badge ${reportType.toLowerCase()}">${reportType === 'FOUND' ? 'FOUND ITEM' : 'LOST ITEM'}</span>
                <span class="card-status ${isResolved ? 'resolved' : ''}">${statusLabel}</span>
            </div>
            <div class="card-body">
                <div class="card-meta"><span class="card-category">${escapeHTML(item.category || 'Other')}</span><span>${escapeHTML(date)}</span></div>
                <h3>${safeName}</h3>
                <p class="card-location"><span aria-hidden="true">@</span><span>${escapeHTML(item.location || 'Location not provided')}</span></p>
                ${description ? `<p class="card-description">${escapeHTML(truncatedDescription)}</p>` : ''}
                ${actions}
            </div>
        </article>`;
    }).join('');
}

async function getErrorMessage(response) {
    try {
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            const data = await response.json();
            return data.detail || data.message || data.error || data.title || `Request failed (${response.status})`;
        }
        const text = (await response.text()).trim();
        return text && text.length < 300 ? text : `Request failed (${response.status}). Please try again.`;
    } catch {
        return `Request failed (${response.status}). Please try again.`;
    }
}

async function loadItems() {
    itemsMessage.textContent = '';
    try {
        const response = await fetch(API_URL, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        const data = await response.json();
        allItems = Array.isArray(data) ? data : [];
        populateCategories();
        updateStats();
        renderItems();
    } catch (error) {
        itemsMessage.textContent = `Could not load reports. ${error.message || 'Check that the FindBackWeb server is running, then refresh.'}`;
        resultsSummary.textContent = 'Reports unavailable';
        itemsGrid.innerHTML = '<div class="empty-state"><div class="empty-icon" aria-hidden="true">!</div><h3>We couldn\'t load the board</h3><p>Check that FindBackWeb is running at localhost:8080, then press Refresh.</p></div>';
    }
}

function showFormMessage(message, type) {
    formMessage.textContent = message;
    formMessage.className = `form-message ${type || ''}`.trim();
}

function clearTemporaryPhotoPreview() {
    if (photoPreviewUrl) {
        URL.revokeObjectURL(photoPreviewUrl);
        photoPreviewUrl = null;
    }
    selectedPhoto = null;
    itemPhoto.value = '';
}

function hidePhotoPreview() {
    photoPreview.removeAttribute('src');
    photoFileName.textContent = '';
    photoPreviewContainer.hidden = true;
}

function displayCurrentPhoto() {
    const path = safeImagePath(existingPhotoPath);
    if (!editingItemId || !path || removeExistingPhoto) {
        hidePhotoPreview();
        return;
    }
    photoPreview.src = path;
    photoFileName.textContent = 'Current saved photo  |  keep it or remove it';
    photoPreviewContainer.hidden = false;
}

function resetToNewReport(message = '') {
    clearTemporaryPhotoPreview();
    hidePhotoPreview();
    editingItemId = null;
    existingPhotoPath = null;
    removeExistingPhoto = false;
    reportForm.reset();
    reportFormTitle.textContent = 'Submit a report';
    cancelEditButton.hidden = true;
    submitButton.innerHTML = 'Submit report <span aria-hidden="true">-&gt;</span>';
    showFormMessage(message, '');
}

itemPhoto.addEventListener('change', () => {
    const file = itemPhoto.files && itemPhoto.files[0];
    clearTemporaryPhotoPreview();
    if (!file) {
        displayCurrentPhoto();
        return;
    }
    if (!ALLOWED_PHOTO_TYPES.has(file.type)) {
        itemPhoto.value = '';
        displayCurrentPhoto();
        showFormMessage('Please choose a JPG, PNG, or WebP image.', 'error');
        return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
        itemPhoto.value = '';
        displayCurrentPhoto();
        showFormMessage('The image must be 5 MB or smaller.', 'error');
        return;
    }

    selectedPhoto = file;
    removeExistingPhoto = false;
    photoPreviewUrl = URL.createObjectURL(file);
    photoPreview.src = photoPreviewUrl;
    photoFileName.textContent = `${file.name}  |  ${(file.size / 1024 / 1024).toFixed(2)} MB`;
    photoPreviewContainer.hidden = false;
    showFormMessage('', '');
});

removePhotoButton.addEventListener('click', () => {
    if (selectedPhoto) {
        clearTemporaryPhotoPreview();
        displayCurrentPhoto();
        showFormMessage('New photo selection cleared.', '');
        return;
    }
    if (editingItemId && existingPhotoPath && !removeExistingPhoto) {
        removeExistingPhoto = true;
        existingPhotoPath = null;
        hidePhotoPreview();
        showFormMessage('The current photo will be removed when you save your changes.', '');
        return;
    }
    clearTemporaryPhotoPreview();
    hidePhotoPreview();
    showFormMessage('Photo removed. You can continue without a photo.', '');
});

function startEditing(button) {
    const id = Number(button.dataset.id);
    const item = allItems.find(entry => Number(entry.id) === id);
    if (!item) return;

    clearTemporaryPhotoPreview();
    hidePhotoPreview();
    editingItemId = id;
    existingPhotoPath = safeImagePath(item.imagePath) || null;
    removeExistingPhoto = false;

    document.getElementById('reportType').value = String(item.reportType || 'LOST').toUpperCase();
    const categorySelect = document.getElementById('category');
    const categoryValue = String(item.category || 'Other');
    if (![...categorySelect.options].some(option => option.value === categoryValue)) {
        categorySelect.add(new Option(categoryValue, categoryValue));
    }
    categorySelect.value = categoryValue;
    document.getElementById('itemName').value = item.itemName || '';
    document.getElementById('location').value = item.location || '';
    document.getElementById('itemDate').value = item.itemDate || '';
    document.getElementById('description').value = item.description || '';

    reportFormTitle.textContent = `Edit report #${id}`;
    submitButton.innerHTML = 'Save changes <span aria-hidden="true">✓</span>';
    cancelEditButton.hidden = false;
    if (existingPhotoPath) displayCurrentPhoto();
    showFormMessage(`Editing report #${id}. Your changes will be saved to this existing report.`, '');
    document.getElementById('report').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

cancelEditButton.addEventListener('click', () => {
    resetToNewReport('Editing cancelled. Your report has not been changed.');
});

reportForm.addEventListener('submit', async event => {
    event.preventDefault();
    showFormMessage('', '');

    if (!currentUser) {
        showFormMessage('Please sign in or create an account before submitting a report.', 'error');
        openAuthPanel('register');
        return;
    }

    const reportType = document.getElementById('reportType').value;
    const itemName = document.getElementById('itemName').value.trim();
    const category = document.getElementById('category').value.trim();
    const location = document.getElementById('location').value.trim();
    const description = document.getElementById('description').value.trim();
    if (!itemName || !category || !location) {
        showFormMessage('Please complete the item name, category, and location.', 'error');
        return;
    }
    if (selectedPhoto && (!ALLOWED_PHOTO_TYPES.has(selectedPhoto.type) || selectedPhoto.size > MAX_PHOTO_BYTES)) {
        showFormMessage('Please choose a JPG, PNG, or WebP photo no larger than 5 MB.', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('reportType', reportType);
    formData.append('itemName', itemName);
    formData.append('category', category);
    formData.append('location', location);
    formData.append('description', description);
    const itemDate = document.getElementById('itemDate').value;
    if (itemDate) formData.append('itemDate', itemDate);
    if (selectedPhoto) formData.append('photo', selectedPhoto, selectedPhoto.name);

    const isEditing = editingItemId !== null;
    const savedIdBeforeRequest = editingItemId;
    if (isEditing) formData.append('removePhoto', removeExistingPhoto ? 'true' : 'false');

    submitButton.disabled = true;
    cancelEditButton.disabled = true;
    submitButton.innerHTML = isEditing ? 'Saving changes…' : 'Submitting…';
    try {
        const url = isEditing ? `${API_URL}/${encodeURIComponent(editingItemId)}` : API_URL;
        const response = await secureFetch(url, {
            method: isEditing ? 'PUT' : 'POST',
            body: formData,
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        const savedItem = await response.json();
        const referenceId = savedItem.id || savedIdBeforeRequest || '';
        resetToNewReport(`${isEditing ? 'Changes saved' : 'Report submitted'} successfully${referenceId ? ` (reference #${referenceId})` : ''}.`);
        await loadItems();
        formMessage.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (error) {
        showFormMessage(`Could not ${isEditing ? 'save the changes' : 'submit the report'}. ${error.message || 'Please try again.'}`, 'error');
    } finally {
        submitButton.disabled = false;
        cancelEditButton.disabled = false;
        submitButton.innerHTML = editingItemId !== null
            ? 'Save changes <span aria-hidden="true">✓</span>'
            : 'Submit report <span aria-hidden="true">-&gt;</span>';
    }
});

async function showMatches(button) {
    const id = button.dataset.id;
    const resultBox = document.getElementById(`matches-${id}`);
    if (!resultBox) return;
    if (button.getAttribute('aria-expanded') === 'true') {
        button.setAttribute('aria-expanded', 'false');
        button.innerHTML = '* Check matches';
        resultBox.innerHTML = '';
        return;
    }
    button.setAttribute('aria-expanded', 'true');
    button.innerHTML = 'Loading matches…';
    resultBox.innerHTML = '<p>Looking for open reports of the opposite type…</p>';
    try {
        const response = await fetch(`${API_URL}/${encodeURIComponent(id)}/matches`, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        const matches = await response.json();
        if (!Array.isArray(matches) || matches.length === 0) {
            resultBox.innerHTML = '<p>No possible matches found yet. New reports may help.</p>';
            return;
        }
        resultBox.innerHTML = '<p>Possible matches — compare the details before contacting anyone.</p>' + matches.map(result => {
            const item = result.item || result;
            const score = Math.max(0, Math.min(100, Number(result.score) || 0));
            const imagePath = safeImagePath(item.imagePath);
            return `<div class="possible-match-card"><span class="match-score">${score}% detail similarity</span><h4>${escapeHTML(item.itemName || 'Unnamed item')}</h4><p>${escapeHTML(item.reportType || 'Report')}  |  ${escapeHTML(item.category || 'Other')}</p><p>@${escapeHTML(item.location || 'Location not provided')}</p>${item.description ? `<p>${escapeHTML(item.description)}</p>` : ''}${imagePath ? `<p><a href="${escapeHTML(imagePath)}" target="_blank" rel="noopener noreferrer">View item photo ↗</a></p>` : ''}<p>Similarity is a simple comparison of report details, not proof that these items are the same.</p></div>`;
        }).join('');
    } catch (error) {
        resultBox.innerHTML = `<p>Couldn't check possible matches. ${escapeHTML(error.message || 'Please try again.')}</p>`;
    } finally {
        button.innerHTML = '* Hide matches';
    }
}

async function resolveItem(button) {
    const id = button.dataset.id;
    const item = allItems.find(entry => Number(entry.id) === Number(id));
    if (!item) return;
    if (!window.confirm(`Mark “${item.itemName || 'this item'}” as resolved?`)) return;
    button.disabled = true;
    button.textContent = 'Updating…';
    try {
        const response = await secureFetch(`${API_URL}/${encodeURIComponent(id)}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify({ status: 'RESOLVED' })
        });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        await loadItems();
    } catch (error) {
        window.alert(`Could not update the report. ${error.message || 'Please try again.'}`);
        button.disabled = false;
        button.textContent = 'Mark resolved';
    }
}

itemsGrid.addEventListener('click', event => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    if (button.dataset.action === 'edit') startEditing(button);
    if (button.dataset.action === 'matches') showMatches(button);
    if (button.dataset.action === 'resolve') resolveItem(button);
});

[searchInput, typeFilter, categoryFilter, locationFilter].forEach(control => {
    control.addEventListener(control.tagName === 'INPUT' ? 'input' : 'change', renderItems);
});
refreshButton.addEventListener('click', loadItems);
clearFiltersButton.addEventListener('click', () => {
    searchInput.value = '';
    typeFilter.value = 'ALL';
    categoryFilter.value = 'ALL';
    locationFilter.value = '';
    renderItems();
});

// Account and CSRF support. Passwords are sent only to the local same-origin API over this session.
function getCookie(name) {
    const prefix = `${name}=`;
    const entry = document.cookie.split(';').map(value => value.trim()).find(value => value.startsWith(prefix));
    return entry ? decodeURIComponent(entry.slice(prefix.length)) : null;
}

async function ensureCsrfCookie() {
    if (getCookie('XSRF-TOKEN')) return;
    const response = await fetch('/api/auth/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error(await getErrorMessage(response));
}

async function secureFetch(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        await ensureCsrfCookie();
        const token = getCookie('XSRF-TOKEN');
        if (token) headers.set('X-XSRF-TOKEN', token);
    }
    return fetch(url, { ...options, headers, credentials: 'same-origin' });
}

function showAuthMessage(message, type = '') {
    authMessage.textContent = message;
    authMessage.className = `auth-message ${type}`.trim();
}

function setAuthMode(isRegister) {
    registerMode = Boolean(isRegister);
    authNameField.hidden = !registerMode;
    authDisplayName.required = registerMode;
    authPassword.minLength = registerMode ? 8 : 1;
    authPassword.autocomplete = registerMode ? 'new-password' : 'current-password';
    authPanelTitle.textContent = registerMode ? 'Create your account' : 'Welcome back';
    authPanelDescription.textContent = registerMode
        ? 'Create an account to submit reports and manage the items you report.'
        : 'Sign in to submit reports and manage items you reported.';
    authSubmitButton.textContent = registerMode ? 'Create account' : 'Sign in';
    authModeToggle.textContent = registerMode ? 'Already have an account? Sign in' : 'New here? Create an account';
    showAuthMessage('');
}

function openAuthPanel(mode = 'login') {
    authPanel.hidden = false;
    setAuthMode(mode === 'register');
    authPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (mode === 'register') authDisplayName.focus({ preventScroll: true });
    else authEmail.focus({ preventScroll: true });
}

function updateAuthUI() {
    accountButton.textContent = currentUser ? `My account: ${currentUser.displayName}` : 'Sign in';
    accountButton.title = currentUser ? currentUser.email : 'Sign in or create an account';
    logoutButton.hidden = !currentUser;
    myReportsLink.textContent = showingMyReports ? 'Browse all reports' : 'My reports';
    myReportsLink.setAttribute('aria-pressed', String(showingMyReports));
}

async function refreshCurrentUser() {
    try {
        const response = await fetch('/api/auth/me', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        const user = await response.json();
        currentUser = user && user.authenticated ? user : null;
    } catch {
        currentUser = null;
    }
    updateAuthUI();
}

accountButton.addEventListener('click', () => {
    if (currentUser) {
        showingMyReports = true;
        updateAuthUI();
        renderItems();
        document.getElementById('browse').scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else if (authPanel.hidden) {
        openAuthPanel('login');
    } else {
        authPanel.hidden = true;
    }
});

authModeToggle.addEventListener('click', () => setAuthMode(!registerMode));
authCloseButton.addEventListener('click', () => { authPanel.hidden = true; });

myReportsLink.addEventListener('click', event => {
    event.preventDefault();
    if (!currentUser) {
        openAuthPanel('login');
        showAuthMessage('Sign in to view the reports submitted by your account.', 'error');
        return;
    }
    showingMyReports = !showingMyReports;
    updateAuthUI();
    renderItems();
    document.getElementById('browse').scrollIntoView({ behavior: 'smooth', block: 'start' });
});

authForm.addEventListener('submit', async event => {
    event.preventDefault();
    showAuthMessage('');
    const email = authEmail.value.trim();
    const password = authPassword.value;
    const displayName = authDisplayName.value.trim();
    if (registerMode && !displayName) {
        showAuthMessage('Please enter your name.', 'error');
        return;
    }
    authSubmitButton.disabled = true;
    authSubmitButton.textContent = registerMode ? 'Creating account…' : 'Signing in…';
    try {
        const endpoint = registerMode ? '/api/auth/register' : '/api/auth/login';
        const payload = registerMode ? { displayName, email, password } : { email, password };
        const response = await secureFetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        const user = await response.json();
        currentUser = user && user.authenticated ? user : null;
        if (!currentUser) throw new Error('The server did not confirm the signed-in account.');
        authPanel.hidden = true;
        authForm.reset();
        showingMyReports = false;
        updateAuthUI();
        await loadItems();
        if (registerMode) {
            showFormMessage('Account created. You are signed in and can now submit reports.', 'success');
        }
    } catch (error) {
        showAuthMessage(error.message || 'Could not sign in. Please try again.', 'error');
    } finally {
        authSubmitButton.disabled = false;
        authSubmitButton.textContent = registerMode ? 'Create account' : 'Sign in';
    }
});

logoutButton.addEventListener('click', async () => {
    logoutButton.disabled = true;
    try {
        const response = await secureFetch('/api/auth/logout', {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw new Error(await getErrorMessage(response));
        currentUser = null;
        showingMyReports = false;
        resetToNewReport('You have signed out. Sign in to submit or manage reports.');
        updateAuthUI();
        await loadItems();
    } catch (error) {
        window.alert(`Could not sign out. ${error.message || 'Please try again.'}`);
    } finally {
        logoutButton.disabled = false;
    }
});

// Keep the report date limited to today or earlier.
document.getElementById('itemDate').max = new Date().toLocaleDateString('en-CA');
(async function initialiseFindBack() {
    try { await ensureCsrfCookie(); } catch { /* Public browsing still works if account endpoints are temporarily unavailable. */ }
    await refreshCurrentUser();
    await loadItems();
})();
