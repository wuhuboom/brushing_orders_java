import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
  'permission',
  {
    state: () => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes) {
        this.addRoutes = routes
        this.routes = constantRoutes.concat(routes)
      },
      setDefaultRoutes(routes) {
        this.defaultRoutes = constantRoutes.concat(routes)
      },
      setTopbarRoutes(routes) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes) {
        this.sidebarRouters = routes
      },
      generateRoutes(roles) {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const routeData = withLegacyShellRoutes(JSON.parse(JSON.stringify(res.data || [])))
            const sdata = JSON.parse(JSON.stringify(routeData))
            const rdata = JSON.parse(JSON.stringify(routeData))
            const defaultData = JSON.parse(JSON.stringify(routeData))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  let res
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

function withLegacyShellRoutes(routes) {
  const systemRoute = routes.find(route => route.path === '/system' || route.meta?.title === '系统管理')
  if (systemRoute) {
    const legacyGroups = [{
      path: 'user',
      component: 'ParentView',
      redirect: 'noRedirect',
      alwaysShow: true,
      meta: { title: '用户管理', icon: 'user' },
      children: [
        { path: 'orgs', component: 'system/dept/index', name: 'LegacySystemOrgs', meta: { title: '组织管理', icon: 'tree' } },
        { path: 'positions', component: 'system/post/index', name: 'LegacySystemPositions', meta: { title: '职位管理', icon: 'post' } },
        { path: 'users', component: 'system/user/index', name: 'LegacySystemUsers', meta: { title: '用户管理', icon: 'user' } }
      ]
    }, {
      path: 'permission',
      component: 'ParentView',
      redirect: 'noRedirect',
      alwaysShow: true,
      meta: { title: '权限管理', icon: 'validCode' },
      children: [
        { path: 'strategies', component: 'system/strategy/index', name: 'LegacySystemStrategies', meta: { title: '策略管理', icon: 'skill' } },
        { path: 'roles', component: 'system/role/index', name: 'LegacySystemRoles', meta: { title: '角色管理', icon: 'peoples' } },
        { path: 'groups', component: 'system/group/index', name: 'LegacySystemGroups', meta: { title: '分组管理', icon: 'people' } }
      ]
    }, {
      path: 'file',
      component: 'ParentView',
      redirect: 'noRedirect',
      alwaysShow: true,
      meta: { title: '文件管理', icon: 'folder' },
      children: [
        { path: 'fileReferences', component: 'system/file/reference', name: 'LegacySystemFileReferences', meta: { title: '文件引用', icon: 'link' } },
        { path: 'files', component: 'system/file/index', name: 'LegacySystemFiles', meta: { title: '文件管理', icon: 'folder' } }
      ]
    }, {
      path: 'operationLog',
      component: 'ParentView',
      redirect: 'noRedirect',
      alwaysShow: true,
      meta: { title: '操作日志', icon: 'log' },
      children: [
        { path: 'operationLogs', component: 'monitor/operlog/index', name: 'LegacySystemOperlog', meta: { title: '操作日志', icon: 'log' } },
        { path: 'userLoginLogs', component: 'monitor/logininfor/index', name: 'LegacySystemLoginlog', meta: { title: '登录日志', icon: 'logininfor' } }
      ]
    }]

    const existingByPath = new Map((systemRoute.children || []).map(item => [item.path, item]))
    systemRoute.children = legacyGroups.map(group => mergeLegacyGroup(existingByPath.get(group.path), group))
  }

  addMissingRoute(routes, {
    path: '/system/user',
    component: 'Layout',
    hidden: true,
    children: [
      { path: 'orgs', component: 'system/dept/index', name: 'LegacyDirectSystemOrgs', meta: { title: '组织管理', icon: 'tree' } },
      { path: 'positions', component: 'system/post/index', name: 'LegacyDirectSystemPositions', meta: { title: '职位管理', icon: 'post' } },
      { path: 'users', component: 'system/user/index', name: 'LegacyDirectSystemUsers', meta: { title: '用户管理', icon: 'user' } }
    ]
  })
  addMissingRoute(routes, {
    path: '/system/permission',
    component: 'Layout',
    hidden: true,
      children: [
        { path: 'strategies', component: 'system/strategy/index', name: 'LegacyDirectSystemStrategies', meta: { title: '策略管理', icon: 'skill' } },
        { path: 'roles', component: 'system/role/index', name: 'LegacyDirectSystemRoles', meta: { title: '角色管理', icon: 'peoples' } },
        { path: 'groups', component: 'system/group/index', name: 'LegacyDirectSystemGroups', meta: { title: '分组管理', icon: 'people' } }
      ]
    })
  addMissingRoute(routes, {
    path: '/system/file', component: 'Layout', hidden: true,
    children: [
      { path: 'fileReferences', component: 'system/file/reference', name: 'LegacyDirectFileReferences', meta: { title: '文件引用' } },
      { path: 'files', component: 'system/file/index', name: 'LegacyDirectFiles', meta: { title: '文件管理' } }
    ]
  })
  addMissingRoute(routes, {
    path: '/system/operationLog', component: 'Layout', hidden: true,
    children: [
      { path: 'operationLogs', component: 'monitor/operlog/index', name: 'LegacyDirectOperationLogs', meta: { title: '操作日志' } },
      { path: 'userLoginLogs', component: 'monitor/logininfor/index', name: 'LegacyDirectUserLoginLogs', meta: { title: '登录日志' } }
    ]
  })
  return routes
}

function mergeLegacyGroup(existing, expected) {
  if (!existing) return expected
  const existingChildren = new Map((existing.children || []).map(item => [item.path, item]))
  return {
    ...existing,
    ...expected,
    meta: { ...(existing.meta || {}), ...expected.meta },
    children: expected.children.map(child => ({
      ...(existingChildren.get(child.path) || {}),
      ...child,
      meta: { ...(existingChildren.get(child.path)?.meta || {}), ...child.meta }
    }))
  }
}

function addMissingRoute(routes, route) {
  if (!routes.some(item => item.path === route.path || item.meta?.title === route.meta?.title)) {
    routes.push(route)
  }
}

export default usePermissionStore
