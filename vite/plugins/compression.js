import compression from 'vite-plugin-compression'

export const COMPRESSIBLE_ASSET_FILTER = /\.(js|mjs|json|css)$/i

export default function createCompression(env) {
    const { VITE_BUILD_COMPRESS } = env
    const plugin = []
    if (VITE_BUILD_COMPRESS) {
        const compressList = VITE_BUILD_COMPRESS.split(',')
        if (compressList.includes('gzip')) {
            plugin.push(
                compression({
                    ext: '.gz',
                    filter: COMPRESSIBLE_ASSET_FILTER,
                    deleteOriginFile: false
                })
            )
        }
        if (compressList.includes('brotli')) {
            plugin.push(
                compression({
                    ext: '.br',
                    algorithm: 'brotliCompress',
                    filter: COMPRESSIBLE_ASSET_FILTER,
                    deleteOriginFile: false
                })
            )
        }
    }
    return plugin
}
