// API 封装层 — JWT Token 版本
const BASE = '/api';

// Token 管理
const TokenManager = {
    getToken() {
        return localStorage.getItem('jwt_token');
    },
    setToken(token) {
        localStorage.setItem('jwt_token', token);
    },
    removeToken() {
        localStorage.removeItem('jwt_token');
    },
    getUser() {
        const user = localStorage.getItem('user_info');
        return user ? JSON.parse(user) : null;
    },
    setUser(user) {
        localStorage.setItem('user_info', JSON.stringify(user));
    },
    removeUser() {
        localStorage.removeItem('user_info');
    },
    isLoggedIn() {
        return !!this.getToken();
    },
    logout() {
        this.removeToken();
        this.removeUser();
    }
};

async function request(url, options = {}) {
    const token = TokenManager.getToken();
    const headers = { 'Content-Type': 'application/json' };

    // 添加 JWT Token
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const config = {
        headers,
        ...options
    };
    if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
        config.body = JSON.stringify(config.body);
    }
    // FormData 不设置 Content-Type，让浏览器自动处理
    if (config.body instanceof FormData) {
        delete headers['Content-Type'];
    }

    const res = await fetch(BASE + url, config);
    const data = await res.json();

    // 401 — Token 过期或无效
    if (data.code === 401) {
        TokenManager.logout();
        // 不在登录页则跳转
        if (!window.location.pathname.includes('login') && !window.location.pathname.includes('register')) {
            window.location.href = '/index.html';
        }
        throw new Error(data.message || '请先登录');
    }

    if (data.code !== 200) {
        throw new Error(data.message || '请求失败');
    }
    return data.data;
}

// ========== 用户相关 ==========
const userApi = {
    login: async (username, password) => {
        const data = await request('/user/login', { method: 'POST', body: { username, password } });
        // 登录成功保存 Token 和用户信息
        TokenManager.setToken(data.token);
        TokenManager.setUser({ id: data.userId, username: data.username, role: data.role, avatar: data.avatar });
        return data;
    },
    register: (username, password, phone, email) =>
        request('/user/register', { method: 'POST', body: { username, password, phone, email } }),
    logout: async () => {
        try { await request('/user/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
        TokenManager.logout();
    },
    profile: () => request('/user/profile'),
    info: () => request('/user/info'),
    updateProfile: (data) => request('/user/profile', { method: 'POST', body: data }),
    changePassword: (data) => request('/user/change-password', { method: 'POST', body: data }),
    uploadAvatar: async (file) => {
        const formData = new FormData();
        formData.append('file', file);
        const token = TokenManager.getToken();
        const res = await fetch(BASE + '/user/avatar', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        });
        const data = await res.json();
        if (data.code !== 200) throw new Error(data.message);
        return data.data;
    }
};

// ========== 账号相关 ==========
const accountApi = {
    list: () => request('/account/list'),
    detail: (id) => request('/account/detail/' + id),
    mySales: (status) => request('/account/mySales' + (status ? '?status=' + status : '')),
    retrieve: (id) => request('/account/retrieve/' + id, { method: 'POST' }),
    delete: (id) => request('/account/delete/' + id, { method: 'DELETE' }),
    create: (data) => request('/account/create', { method: 'POST', body: data }),
    uploadImages: async (files) => {
        const formData = new FormData();
        files.forEach(f => formData.append('files', f));
        const token = TokenManager.getToken();
        const res = await fetch(BASE + '/account/uploadImages', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        });
        const data = await res.json();
        if (data.code !== 200) throw new Error(data.message);
        return data.data;
    }
};

// ========== 订单相关 ==========
const orderApi = {
    create: (accountId) => request('/order/create', { method: 'POST', body: { accountId } }),
    createWithBargain: (bargainId) => request('/order/createWithBargain', { method: 'POST', body: { bargainId } }),
    pay: (id) => request('/order/pay/' + id, { method: 'POST' }),
    cancel: (id) => request('/order/cancel/' + id, { method: 'POST' }),
    delete: (id) => request('/order/delete/' + id, { method: 'DELETE' }),
    myOrders: (status) => request('/order/myOrders' + (status ? '?status=' + status : '')),
    detail: (id) => request('/order/detail/' + id)
};

// ========== 还价相关 ==========
const bargainApi = {
    create: (accountId, price) => request('/bargain/create', { method: 'POST', body: { accountId, price } }),
    accept: (id) => request('/bargain/accept/' + id, { method: 'POST' }),
    counter: (id, price) => request('/bargain/counter/' + id, { method: 'POST', body: { price } }),
    buyerAccept: (id) => request('/bargain/buyerAccept/' + id, { method: 'POST' }),
    myBargains: () => request('/bargain/myBargains'),
    receivedBargains: () => request('/bargain/receivedBargains'),
    delete: (id) => request('/bargain/delete/' + id, { method: 'DELETE' })
};

// ========== 登记相关 ==========
const registrationApi = {
    submit: (data) => request('/registration/submit', { method: 'POST', body: data }),
    cancel: (id) => request('/registration/cancel/' + id, { method: 'POST' }),
    myRegistrations: () => request('/registration/myRegistrations'),
    detail: (id) => request('/registration/detail/' + id),
    delete: (id) => request('/registration/delete/' + id, { method: 'DELETE' })
};

// ========== 管理员相关 ==========
const adminApi = {
    login: async (username, password) => {
        const data = await request('/admin/login', { method: 'POST', body: { username, password } });
        TokenManager.setToken(data.token);
        TokenManager.setUser({ id: data.userId, username: data.username, role: data.role, avatar: data.avatar });
        return data;
    },
    registrations: (status) => request('/admin/registrations' + (status ? '?status=' + status : '')),
    registrationDetail: (id) => request('/admin/registration/detail/' + id),
    approve: (id) => request('/admin/approve/' + id, { method: 'POST' }),
    reject: (id) => request('/admin/reject/' + id, { method: 'POST' }),
    deleteRegistration: (id) => request('/admin/registration/delete/' + id, { method: 'DELETE' }),
    logout: async () => {
        try { await request('/admin/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
        TokenManager.logout();
    },
    // 账号管理
    accountDetail: (id) => request('/admin/account/detail/' + id),
    accountUpdate: (data) => request('/admin/account/update', { method: 'PUT', body: data }),
    accountOffline: (id) => request('/admin/account/offline/' + id, { method: 'POST' }),
    accountOnline: (id) => request('/admin/account/online/' + id, { method: 'POST' }),
    // 订单管理
    orders: (status) => request('/admin/orders' + (status ? '?status=' + status : '')),
    orderDetail: (id) => request('/admin/order/detail/' + id),
    orderDelete: (id) => request('/admin/order/delete/' + id, { method: 'DELETE' }),
    // 用户管理
    userList: (page, size, keyword) => request('/admin/users/list?page=' + page + '&size=' + size + (keyword ? '&keyword=' + keyword : '')),
    userGet: (id) => request('/admin/users/' + id),
    userAdd: (data) => request('/admin/users/add', { method: 'POST', body: data }),
    userUpdate: (data) => request('/admin/users/update', { method: 'PUT', body: data }),
    userToggle: (id) => request('/admin/users/toggle/' + id, { method: 'POST' }),
    userDelete: (id) => request('/admin/users/' + id, { method: 'DELETE' })
};
