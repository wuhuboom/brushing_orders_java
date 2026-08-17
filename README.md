# 安装依赖

yarn --registry=https://registry.npmmirror.com

# 启动服务

yarn dev

# 构建测试环境 yarn build:stage

# 构建生产环境 yarn build:prod

# 前端访问地址 http://localhost:80

# 运行时配置

`config/config.js` 是部署时外置配置，不会复制到 `dist` 构建包中。部署新版前端时请保留服务器现有的 `/config/config.js`，首次部署则单独放置该文件并配置 `baseApiUrl`。

```


```
