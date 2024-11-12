module.exports = [
    {
        context: ['/login', '/register', '/reset-password', '/users', '/delete-user', '/settings/**', '/payments/**', '/products/**', '/purchases/**', '/account-balance'],
        target: 'http://192.168.178.100:8000',
        secure: false,
        changeOrigin: true
    }
];
