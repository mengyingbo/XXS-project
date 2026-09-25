// iOS 11 Safari 11 兼容性 polyfill
// 必须在应用入口最前面导入
/* eslint-disable @typescript-eslint/no-explicit-any */

// globalThis (ES2020)
if (typeof (globalThis as any) === 'undefined') {
  const g: any =
    typeof window !== 'undefined'
      ? window
      : typeof self !== 'undefined'
        ? self
        : {}
  ;(g as any).globalThis = g
}

// Array.prototype.flat / flatMap (ES2019)
if (!Array.prototype.flat) {
  Object.defineProperty(Array.prototype, 'flat', {
    configurable: true,
    writable: true,
    value: function (this: any[], depth = 1): any[] {
      const result: any[] = []
      const flatten = (arr: any[], d: number) => {
        for (const item of arr) {
          if (Array.isArray(item) && d > 0) {
            flatten(item, d - 1)
          } else {
            result.push(item)
          }
        }
      }
      flatten(this, depth)
      return result
    }
  })
}

if (!Array.prototype.flatMap) {
  Object.defineProperty(Array.prototype, 'flatMap', {
    configurable: true,
    writable: true,
    value: function (this: any[], callback: (...args: any[]) => any): any[] {
      return this.map(callback).flat()
    }
  })
}

// Object.fromEntries (ES2019)
if (!(Object as any).fromEntries) {
  ;(Object as any).fromEntries = function (entries: Iterable<[string, any]>) {
    const obj: Record<string, any> = {}
    for (const [key, value] of entries) {
      obj[key] = value
    }
    return obj
  }
}

// String.prototype.trimStart / trimEnd (ES2019)
const sp = String.prototype as any
if (!sp.trimStart) {
  sp.trimStart = sp.trimLeft || function (this: string) {
    return this.replace(/^\s+/, '')
  }
}
if (!sp.trimEnd) {
  sp.trimEnd = sp.trimRight || function (this: string) {
    return this.replace(/\s+$/, '')
  }
}

// Promise.prototype.finally (ES2018)
if (!(Promise.prototype as any).finally) {
  ;(Promise.prototype as any).finally = function (this: Promise<any>, callback: () => any) {
    const P = this.constructor as PromiseConstructor
    return this.then(
      (value) => P.resolve(callback()).then(() => value),
      (reason) =>
        P.resolve(callback()).then(() => {
          throw reason
        })
    )
  }
}

export {}
