import re

_TOKEN = re.compile(r"\s+|[^\s]+")


def _is_upper_word(tok: str) -> bool:
    letters = [c for c in tok if c.isalpha()]
    return len(letters) >= 2 and all(c.isupper() for c in letters)


def normalize_text(text: str) -> str:
    """Đổi cụm từ viết hoa toàn bộ (tiêu đề như "LỜI GIỚI THIỆU") sang chữ thường
    để model không đọc từng chữ cái. Từ in hoa đứng một mình (USA, AI) được giữ nguyên."""
    toks = _TOKEN.findall(text)
    out, i = [], 0
    while i < len(toks):
        if _is_upper_word(toks[i]):
            j, words = i, 0
            while j < len(toks) and (toks[j].isspace() or _is_upper_word(toks[j])):
                words += not toks[j].isspace()
                j += 1
                if toks[j - 1].endswith((".", "!", "?")):  # hết câu thì dừng cụm
                    break
            while toks[j - 1].isspace():
                j -= 1
            seg = "".join(toks[i:j])
            out.append(seg.lower() if words >= 2 else seg)
            i = j
        else:
            out.append(toks[i])
            i += 1
    return "".join(out)
