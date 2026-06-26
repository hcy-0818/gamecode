// ========== 全局状态 ==========
let currentUser = null;
let currentDetailAccount = null;
let currentBargainAccountId = null;
let currentCounterBargainId = null;
let currentOrderStatus = 'ALL';

// ========== 初始化 ==========
(async function init() {
    try {
        currentUser = await userApi.info();
        showMainPage();
    } catch (e) {
        showAuthPage();
    }
})();

// ========== 页面切换 ==========
function showAuthPage() {
    document.getElementById('auth-page').classList.add('active');
    document.getElementById('main-page').classList.remove('active');
    currentUser = null;
}

function showMainPage() {
    document.getElementById('auth-page').classList.remove('active');
    document.getElementById('main-page').classList.add('active');
    document.getElementById('nav-username').textContent = currentUser.username;
    switchPage('home');
}

function switchPage(name) {
    document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.content-page').forEach(p => p.classList.remove('active'));
    document.getElementById('page-' + name).classList.add('active');
    document.querySelector('.nav-tab[onclick="switchPage(\'' + name + '\')"]').classList.add('active');
    if (name === 'home') loadHome();
    if (name === 'orders') loadOrders('ALL');
    if (name === 'my') clearMyContent();
}

// ========== 认证相关 ==========
function showAuthTab(tab) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
    document.querySelector('.auth-tab[onclick="showAuthTab(\'' + tab + '\')"]').classList.add('active');
    document.getElementById(tab + '-form').classList.add('active');
    document.getElementById('auth-msg').textContent = '';
}

async function userLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();
    if (!username || !password) return showMsg('auth-msg', '请输入用户名和密码', 'error');
    try {
        currentUser = await userApi.login(username, password);
        showMainPage();
    } catch (e) {
        showMsg('auth-msg', e.message, 'error');
    }
}

async function userRegister() {
    const username = document.getElementById('reg-username').value.trim();
    const password = document.getElementById('reg-password').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    if (!username || !password) return showMsg('auth-msg', '请输入用户名和密码', 'error');
    try {
        await userApi.register(username, password, phone, email);
        showMsg('auth-msg', '注册成功，请登录', 'success');
        showAuthTab('login');
        document.getElementById('login-username').value = username;
    } catch (e) {
        showMsg('auth-msg', e.message, 'error');
    }
}

async function logout() {
    try {
        await fetch('/api/user/logout', { method: 'POST', credentials: 'include' });
    } catch (e) { }
    currentUser = null;
    // 清除所有输入框
    document.getElementById('login-username').value = '';
    document.getElementById('login-password').value = '';
    document.getElementById('auth-msg').textContent = '';
    document.getElementById('auth-msg').className = 'msg';
    showAuthPage();
}

// ========== 首页 ==========
async function loadHome() {
    const container = document.getElementById('account-list');
    try {
        const accounts = await accountApi.list();
        if (accounts.length === 0) {
            container.innerHTML = '<div class="empty"><div class="icon">📭</div><p>暂无在售账号</p></div>';
            return;
        }
        container.innerHTML = accounts.map(a => `
            <div class="account-card" onclick="openDetail(${a.id})">
                <div class="game-name">${esc(a.gameName)}</div>
                <div class="level">等级：${esc(a.accountLevel)}</div>
                <div class="price"><span class="unit">¥</span>${a.price.toFixed(2)}</div>
                <div class="desc">${esc(a.description || '暂无描述')}</div>
                <div class="tags">
                    ${a.allowBargain === 1 ? '<span class="tag tag-bargain">可还价</span>' : ''}
                    <span class="tag tag-status">在售</span>
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<div class="empty"><p>加载失败：' + esc(e.message) + '</p></div>';
    }
}

// ========== 账号详情 ==========
async function openDetail(accountId) {
    try {
        const result = await accountApi.detail(accountId);
        currentDetailAccount = result.account;
        const bargainPrice = result.bargainPrice;
        const body = document.getElementById('detail-body');
        const a = currentDetailAccount;
        body.innerHTML = `
            <div class="detail-header">
                <div class="game-name">${esc(a.gameName)}</div>
                <div class="level">等级：${esc(a.accountLevel)}</div>
                <div class="price">¥${a.price.toFixed(2)}</div>
                ${bargainPrice ? '<div class="bargain-price">您的还价：¥' + bargainPrice.toFixed(2) + '</div>' : ''}
            </div>
            <div class="detail-body">
                <div class="desc">${esc(a.description || '暂无描述')}</div>
            </div>
            <div class="detail-actions">
                ${bargainPrice
                    ? `<button class="btn btn-success" onclick="buyWithBargain()">¥${bargainPrice.toFixed(2)} 立即购买</button>`
                    : `<button class="btn btn-primary" onclick="buyNow()">¥${a.price.toFixed(2)} 立即购买</button>`
                }
                ${a.allowBargain === 1 && !bargainPrice
                    ? `<button class="btn btn-warning" onclick="openBargainModal(${a.id}, ${a.price})">我要还价</button>`
                    : ''}
            </div>
        `;
        openModal('detail-modal');
    } catch (e) {
        alert('加载详情失败：' + e.message);
    }
}

async function buyNow() {
    if (!confirm('确认购买此账号？')) return;
    try {
        await orderApi.create(currentDetailAccount.id);
        closeModal('detail-modal');
        alert('下单成功！');
        loadOrders('ALL');
    } catch (e) {
        alert('下单失败：' + e.message);
    }
}

async function buyWithBargain() {
    if (!confirm('确认以还价价格购买？')) return;
    try {
        await orderApi.create(currentDetailAccount.id);
        closeModal('detail-modal');
        alert('下单成功！');
        loadOrders('ALL');
    } catch (e) {
        alert('下单失败：' + e.message);
    }
}

// ========== 还价弹窗 ==========
function openBargainModal(accountId, price) {
    currentBargainAccountId = accountId;
    const minPrice = (price * 0.8).toFixed(2);
    document.getElementById('bargain-info').innerHTML = `<p>原价：<strong>¥${price.toFixed(2)}</strong></p>`;
    document.getElementById('bargain-price').value = '';
    document.getElementById('bargain-min-tip').textContent = '最低还价：¥' + minPrice + '（原价80%）';
    document.getElementById('bargain-msg').textContent = '';
    openModal('bargain-modal');
}

async function submitBargain() {
    const price = parseFloat(document.getElementById('bargain-price').value);
    if (!price || price <= 0) return showMsg('bargain-msg', '请输入有效金额', 'error');
    try {
        await bargainApi.create(currentBargainAccountId, price);
        closeModal('bargain-modal');
        alert('还价成功，等待卖家回复');
    } catch (e) {
        showMsg('bargain-msg', e.message, 'error');
    }
}

// ========== 卖家还价弹窗 ==========
function openCounterModal(bargainId) {
    currentCounterBargainId = bargainId;
    document.getElementById('counter-price').value = '';
    document.getElementById('counter-msg').textContent = '';
    openModal('counter-modal');
}

async function submitCounter() {
    const price = parseFloat(document.getElementById('counter-price').value);
    if (!price || price <= 0) return showMsg('counter-msg', '请输入有效金额', 'error');
    try {
        await bargainApi.counter(currentCounterBargainId, price);
        closeModal('counter-modal');
        alert('已还价，等待买家确认');
        showReceivedBargains();
    } catch (e) {
        showMsg('counter-msg', e.message, 'error');
    }
}

// ========== 订单 ==========
async function loadOrders(status) {
    currentOrderStatus = status;
    document.querySelectorAll('#page-orders .filter-tab').forEach(t => t.classList.remove('active'));
    const tab = document.querySelector('#page-orders .filter-tab[onclick="loadOrders(\'' + status + '\')"]');
    if (tab) tab.classList.add('active');
    const container = document.getElementById('order-list');
    try {
        const orders = await orderApi.myOrders(status === 'ALL' ? null : status);
        if (orders.length === 0) {
            container.innerHTML = '<div class="empty"><div class="icon">📋</div><p>暂无订单</p></div>';
            return;
        }
        container.innerHTML = orders.map(o => `
            <div class="order-item">
                <div class="order-info">
                    <div class="order-no">订单号：${o.orderNo}</div>
                    <div class="order-title">游戏账号 #${o.accountId}</div>
                    <div class="order-price">¥${o.price.toFixed(2)}</div>
                </div>
                <div style="display:flex;align-items:center;gap:12px;">
                    <span class="order-status status-${o.status}">${statusText(o.status)}</span>
                    <div class="order-actions">
                        ${o.status === 'PENDING_PAYMENT' ? `<button class="btn btn-sm btn-success" onclick="payOrder(${o.id})">支付</button>
                        <button class="btn btn-sm btn-danger" onclick="cancelOrder(${o.id})">取消</button>` : ''}
                        ${o.status === 'CANCELLED' ? `<button class="btn btn-sm btn-danger" onclick="deleteOrder(${o.id})">删除</button>` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<div class="empty"><p>加载失败：' + esc(e.message) + '</p></div>';
    }
}

async function payOrder(id) {
    try { await orderApi.pay(id); alert('支付成功'); loadOrders(currentOrderStatus); } catch (e) { alert(e.message); }
}

async function cancelOrder(id) {
    if (!confirm('确认取消订单？')) return;
    try { await orderApi.cancel(id); loadOrders(currentOrderStatus); } catch (e) { alert(e.message); }
}

async function deleteOrder(id) {
    if (!confirm('确认删除订单？')) return;
    try { await orderApi.delete(id); loadOrders(currentOrderStatus); } catch (e) { alert(e.message); }
}

function statusText(s) {
    const map = { PENDING_PAYMENT: '待付款', PAID: '已支付', CANCELLED: '已取消' };
    return map[s] || s;
}

// ========== 我的页面 ==========
function clearMyContent() {
    document.getElementById('my-content').innerHTML = '<div class="empty"><p>请点击上方菜单查看</p></div>';
}

async function showMySales() {
    const container = document.getElementById('my-content');
    container.innerHTML = `
        <h3>我的出售</h3>
        <div class="filter-tabs">
            <button class="filter-tab active" onclick="loadMySales('ALL')">全部</button>
            <button class="filter-tab" onclick="loadMySales('LISTED')">上架中</button>
            <button class="filter-tab" onclick="loadMySales('SOLD')">已出售</button>
            <button class="filter-tab" onclick="loadMySales('RETRIEVED')">已取回</button>
        </div>
        <div id="my-sales-list"></div>
    `;
    loadMySales('ALL');
}

async function loadMySales(status) {
    const sales = await accountApi.mySales(status === 'ALL' ? null : status);
    const list = document.getElementById('my-sales-list');
    if (sales.length === 0) {
        list.innerHTML = '<div class="empty"><p>暂无记录</p></div>';
        return;
    }
    list.innerHTML = sales.map(a => `
        <div class="order-item">
            <div class="order-info">
                <div class="order-title">${esc(a.gameName)}</div>
                <div style="color:#666;font-size:13px;">等级：${esc(a.accountLevel)} | ¥${a.price.toFixed(2)}</div>
            </div>
            <div style="display:flex;align-items:center;gap:10px;">
                <span class="order-status status-${a.status === 'LISTED' ? 'PAID' : a.status === 'SOLD' ? 'PAID' : 'CANCELLED'}">${saleStatusText(a.status)}</span>
                ${a.status === 'LISTED' ? `<button class="btn btn-sm btn-danger" onclick="retrieveAccount(${a.id})">取回</button>` : ''}
            </div>
        </div>
    `).join('');
}

async function retrieveAccount(id) {
    if (!confirm('确认取回此账号？取回后将不再展示')) return;
    try { await accountApi.retrieve(id); alert('取回成功'); showMySales(); } catch (e) { alert(e.message); }
}

function saleStatusText(s) {
    const map = { LISTED: '上架中', SOLD: '已出售', RETRIEVED: '已取回' };
    return map[s] || s;
}

async function showMyRegistrations() {
    const container = document.getElementById('my-content');
    container.innerHTML = `
        <h3>我的登记</h3>
        <button class="btn btn-primary" style="width:auto;margin:12px 0;" onclick="openModal('register-account-modal')">+ 我要登记</button>
        <div id="my-reg-list"></div>
    `;
    loadMyRegistrations();
}

async function loadMyRegistrations() {
    try {
        const list = await registrationApi.myRegistrations();
        const el = document.getElementById('my-reg-list');
        if (!el) return;
        if (list.length === 0) {
            el.innerHTML = '<div class="empty"><p>暂无登记记录</p></div>';
            return;
        }
        el.innerHTML = list.map(r => `
            <div class="order-item">
                <div class="order-info">
                    <div class="order-title">${esc(r.gameName)}</div>
                    <div style="color:#666;font-size:13px;">等级：${esc(r.accountLevel)} | ¥${r.price.toFixed(2)}</div>
                </div>
                <div style="display:flex;align-items:center;gap:10px;">
                    <span class="order-status status-${regStatusClass(r.status)}">${regStatusText(r.status)}</span>
                    ${r.status === 'PENDING' ? `<button class="btn btn-sm btn-danger" onclick="cancelRegistration(${r.id})">取消</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (e) {
        const el = document.getElementById('my-reg-list');
        if (el) el.innerHTML = '<div class="empty"><p>加载失败</p></div>';
    }
}

async function cancelRegistration(id) {
    if (!confirm('确认取消登记？')) return;
    try { await registrationApi.cancel(id); loadMyRegistrations(); } catch (e) { alert(e.message); }
}

async function submitRegistration() {
    const data = {
        gameName: document.getElementById('reg-game-name').value.trim(),
        accountLevel: document.getElementById('reg-account-level').value.trim(),
        price: parseFloat(document.getElementById('reg-price').value),
        description: document.getElementById('reg-desc').value.trim(),
        allowBargain: document.getElementById('reg-allow-bargain').checked ? 1 : 0,
        images: document.getElementById('reg-images').value.trim()
    };
    if (!data.gameName || !data.price) return showMsg('reg-account-msg', '请填写游戏名称和售价', 'error');
    try {
        await registrationApi.submit(data);
        closeModal('register-account-modal');
        alert('登记提交成功，等待管理员审批');
        loadMyRegistrations();
    } catch (e) {
        showMsg('reg-account-msg', e.message, 'error');
    }
}

function regStatusText(s) {
    const map = { PENDING: '等待审批', APPROVED: '登记成功', REJECTED: '已驳回', CANCELLED: '已取消' };
    return map[s] || s;
}

function regStatusClass(s) {
    const map = { PENDING: 'PENDING_PAYMENT', APPROVED: 'PAID', REJECTED: 'CANCELLED', CANCELLED: 'CANCELLED' };
    return map[s] || '';
}

async function showMyBargains() {
    const container = document.getElementById('my-content');
    container.innerHTML = '<h3>我的还价</h3><div id="my-bargain-list"></div>';
    try {
        const list = await bargainApi.myBargains();
        const el = document.getElementById('my-bargain-list');
        if (list.length === 0) {
            el.innerHTML = '<div class="empty"><p>暂无还价记录</p></div>';
            return;
        }
        el.innerHTML = list.map(b => `
            <div class="bargain-item">
                <div class="bargain-header">
                    <strong>账号 #${b.accountId}</strong>
                    <span class="order-status status-${bargainStatusClass(b.status)}">${bargainStatusText(b.status)}</span>
                </div>
                <div class="bargain-prices">
                    <span>我的出价：¥${b.buyerPrice.toFixed(2)}</span>
                    ${b.sellerPrice ? '<span>卖家还价：¥' + b.sellerPrice.toFixed(2) + '</span>' : ''}
                </div>
                <div class="bargain-actions">
                    ${b.status === 'PENDING' && b.sellerPrice ? `<button class="btn btn-sm btn-success" onclick="buyerAcceptBargain(${b.id})">接受此价格</button>` : ''}
                    ${b.status === 'ACCEPTED' ? `<button class="btn btn-sm btn-primary" onclick="openDetail(${b.accountId})">查看商品</button>` : ''}
                    <button class="btn btn-sm btn-outline" onclick="openDetail(${b.accountId})">查看商品</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        document.getElementById('my-bargain-list').innerHTML = '<div class="empty"><p>加载失败</p></div>';
    }
}

async function buyerAcceptBargain(id) {
    try {
        await bargainApi.buyerAccept(id);
        alert('已接受还价价格');
        showMyBargains();
    } catch (e) { alert(e.message); }
}

async function showReceivedBargains() {
    const container = document.getElementById('my-content');
    container.innerHTML = '<h3>收到还价</h3><div id="received-bargain-list"></div>';
    try {
        const list = await bargainApi.receivedBargains();
        const el = document.getElementById('received-bargain-list');
        if (list.length === 0) {
            el.innerHTML = '<div class="empty"><p>暂无收到还价</p></div>';
            return;
        }
        el.innerHTML = list.map(b => `
            <div class="bargain-item">
                <div class="bargain-header">
                    <strong>账号 #${b.accountId}</strong>
                    <span class="order-status status-${bargainStatusClass(b.status)}">${bargainStatusText(b.status)}</span>
                </div>
                <div class="bargain-prices">
                    <span>买家出价：¥${b.buyerPrice.toFixed(2)}</span>
                    ${b.sellerPrice ? '<span>我的还价：¥' + b.sellerPrice.toFixed(2) + '</span>' : ''}
                </div>
                <div class="bargain-actions">
                    ${b.status === 'PENDING' && !b.sellerPrice ? `<button class="btn btn-sm btn-success" onclick="acceptBargain(${b.id})">同意</button>
                    <button class="btn btn-sm btn-warning" onclick="openCounterModal(${b.id})">还价</button>` : ''}
                    <button class="btn btn-sm btn-outline" onclick="openDetail(${b.accountId})">查看商品</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        document.getElementById('received-bargain-list').innerHTML = '<div class="empty"><p>加载失败</p></div>';
    }
}

async function acceptBargain(id) {
    try { await bargainApi.accept(id); alert('已同意还价'); showReceivedBargains(); } catch (e) { alert(e.message); }
}

function bargainStatusText(s) {
    const map = { PENDING: '协商中', ACCEPTED: '已达成', REJECTED: '已拒绝' };
    return map[s] || s;
}

function bargainStatusClass(s) {
    const map = { PENDING: 'PENDING_PAYMENT', ACCEPTED: 'PAID', REJECTED: 'CANCELLED' };
    return map[s] || '';
}

// ========== 弹窗工具 ==========
function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// ========== 工具函数 ==========
function showMsg(id, msg, type) {
    const el = document.getElementById(id);
    el.textContent = msg;
    el.className = 'msg msg-' + type;
}

function esc(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// 点击弹窗外部关闭
window.onclick = function(e) {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('active');
    }
};