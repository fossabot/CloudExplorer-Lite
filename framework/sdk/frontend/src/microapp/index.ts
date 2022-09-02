import type { Module } from "@commons/api/module";
import { endsWith, trimEnd, trimStart } from "lodash";
import type { MicroApp } from "@micro-zoe/micro-app";
import type { App } from "vue";
import { store } from "@commons/stores";
import microApp from "@micro-zoe/micro-app";
import { useModuleStore } from "@commons/stores/modules/module";

declare global {
  interface Window {
    rootMicroApp: RootMicroApp;
    [index: string]: any;
  }
}
class RootMicroApp {
  /**
   *获取模块信息
   */
  getModules: () => Promise<Array<Module>> | Array<Module>;
  /**
   * microApp对象
   */
  microApp: MicroApp;
  /**
   * 插件modules
   */
  modules: any = {};

  constructor(
    getModules: () => Promise<Array<Module>> | Array<Module>,
    microApp: MicroApp
  ) {
    this.getModules = getModules;
    this.microApp = microApp;
    window.rootMicroApp = this;
  }
  /**
   *解决microApp路径携带子模块名称问题
   * @param code       子模块代码
   * @param moduleName 子模块名称
   * @param moduleUrl  子模块url
   * @returns          去除模块名称的代码
   */
  resetCode(code: string, baseName: string, moduleUrl: string) {
    // 拼接模块url和模块名称
    const totalUrl =
      moduleUrl + (endsWith(moduleUrl, "/") ? "" : "/") + baseName + "/";
    const parsedName = this.regScapeStr("/" + baseName + "/");
    // 生成正则
    const regx = `(from|import)((\\s*['"])|(\\S*['"]))(${parsedName})`;

    // 将子模块的引入都替换掉
    const newCode = code.replace(new RegExp(regx, "g"), (all: string) => {
      const result = all.replace("/" + baseName + "/", totalUrl);
      return result;
    });
    console.log(newCode);
    return newCode;
  }

  regScapeStr(str: string): string {
    return str.replace(/[-/\\^$*+?.()|[\]{}]/g, "\\$&");
  }

  install(app?: App) {
    const getModules = this.getModules();
    if (getModules instanceof Array<Module>) {
      this.setModules(getModules);
    }
    this.microApp.start({ plugins: { modules: this.modules } });
  }

  /**
   * 根据接口返回数据获取运行中的模块,替换代码
   */
  async updateModule() {
    this.setModules(await this.getModules());
  }

  /**
   * 修改模块
   * @param modules 模块数据
   */
  setModules(modules: Array<Module>) {
    modules.forEach((module) => {
      // 去掉模块名称前后斜杠/
      const baseName = trimStart(trimEnd("/" + module.name, "/"), "/");
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const _self = this;
      this.modules[baseName] = [
        {
          loader(code: string) {
            return _self.resetCode(code, baseName, module.basePath);
          },
        },
      ];
    });
  }
}

export function setupMicroApp(app: App<Element>) {
  const moduleStore = useModuleStore(store);
  app.use(
    new RootMicroApp(async () => {
      await moduleStore.refreshModules();
      console.log(moduleStore.runningModules);
      return moduleStore.runningModules;
    }, microApp)
  );
}
