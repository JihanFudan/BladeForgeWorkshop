#!/usr/bin/env python3
"""校验《锻刀工坊游玩手册》(GuideBookItem.PAGES) 的分页会不会被游戏裁掉。

原版 `BookViewScreen` 渲染书页时只画 `TEXT_HEIGHT / font.lineHeight = 128 / 9 = 14` 行，
每行宽 `TEXT_WIDTH = 114` 像素；多出来的行**直接不显示**（不报错、没有省略号），
过长的行还会自动折行、把后面的内容顶出页面。所以约定：

  * 每页最多 13 行（含标题行和空行），给折行留 1 行余量；
  * 每行估算宽度不超过 110 像素（全角 9px、半角 7px、空格 4px）。

改完手册文案后在项目根目录跑一次：`python3 animation_tools/book_layout_check.py`
"""
import re
import sys
import unicodedata

SRC = 'src/main/java/cn/blockforge/generated/slashbladereshslashblad/item/GuideBookItem.java'
MAX_LINES = 13      # 页面行数上限（14 行就会顶到边界，留 1 行余量）
MAX_WIDTH = 110     # 每行估算宽度上限（原版 114px，留 4px 余量）

ESCAPES = {'n': '\n', 't': '\t', '"': '"', '\\': '\\'}


def char_width(c):
    """估一个字符的渲染宽度：全角按 9px，半角按 7px，空格 4px。"""
    if c == ' ':
        return 4
    code = ord(c)
    full_width = (unicodedata.east_asian_width(c) in ('W', 'F')
                  or 0x2E80 <= code <= 0x9FFF      # 中日韩
                  or 0x2460 <= code <= 0x24FF      # ①②③ 带圈数字
                  or 0x2150 <= code <= 0x218F)     # 罗马数字等
    return 9 if full_width else 7


def page_lines(literal):
    """把 Java 字符串字面量还原成真实文本，再按 \n 切成页面行。"""
    text = re.sub(r'\\(.)', lambda m: ESCAPES.get(m.group(1), m.group(1)), literal)
    return text.split('\n')


def pages_of(source):
    start = source.index('PAGES = List.of(')
    end = source.index('\n    );', start)
    return re.findall(r'"((?:[^"\\]|\\.)*)"', source[start:end])


def main():
    src = open(SRC, encoding='utf-8').read()
    pages = pages_of(src)
    problems = 0
    for index, literal in enumerate(pages, 1):
        lines = page_lines(literal)
        bad = []
        if len(lines) > MAX_LINES:
            bad.append('行数 %d > %d（多出来的行在游戏里看不到）' % (len(lines), MAX_LINES))
        for row, line in enumerate(lines, 1):
            width = sum(char_width(c) for c in line)
            if width > MAX_WIDTH:
                bad.append('第 %d 行约 %dpx（> %d）：%s' % (row, width, MAX_WIDTH, line))
        if bad:
            problems += 1
            print('页 %2d（%s）%d 行 ✗' % (index, lines[0], len(lines)))
            for item in bad:
                print('      - ' + item)
    print('共 %d 页，其中 %d 页会被裁字' % (len(pages), problems))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
