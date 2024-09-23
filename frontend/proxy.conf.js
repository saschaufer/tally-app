module.exports = [
    /*{
        context: ['/**'],
        target: 'http://192.168.178.100:4200',
        secure: false,
        changeOrigin: true
    },*/
    {
        context: ['/login', '/register', '/settings/**', '/payments/**', '/products/**', '/purchases/**', '/account-balance'],
        target: 'http://192.168.178.100:8000',
        secure: false,
        changeOrigin: true
    }
];
