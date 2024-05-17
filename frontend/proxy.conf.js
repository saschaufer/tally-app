module.exports = [
    /*{
        context: ['/**'],
        target: 'http://192.168.0.218:4200',
        secure: false,
        changeOrigin: true
    },*/
    {
        context: ['/login', '/register'],
        target: 'http://192.168.0.218:8000',
        secure: false,
        changeOrigin: true
    }
];
