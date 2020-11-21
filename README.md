敏感词过滤
===
## 简介
* 本项目参考网上思路、DFA(Deterministic Finite Automaton)确定有穷自动机实现算法。
* 预先将敏感词解析成字典保存到内存中，然后对文本高效搜索，实现敏感词过滤。
## 环境及依赖
Jdk 1.8
## 运行步骤
执行 test 包下的测试方法
## 目录结构描述
``` lua
sensitive
└── filter
    └── SensitiveWordFilters             程序主逻辑
```
