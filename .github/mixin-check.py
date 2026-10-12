"""Checks the mod's mixins against two versions of the game, for api-diff's mixins job.

python3 mixin-check.py targets <source dirs...>
    Prints the binary names of every class a mixin targets.
python3 mixin-check.py check <old dir> <new dir> <source dirs...>
    Each dir holds members.txt (javap -p -s of the targets) and calls.txt (api-calls.py of javap -c -p of the targets).
    Prints, per mixin, what a newer game breaks or changes: a missing target class, method, field or @At target, an
    @At target whose count changed (ordinals may shift), and the call summary of every targeted method that changed
    (an @Overwrite or a handler there may need the new behaviour). Nothing is printed for a mixin that still matches.
"""
import collections
import os
import re
import sys

HANDLERS = ("Inject", "Redirect", "ModifyVariable", "ModifyArg", "ModifyArgs", "ModifyConstant",
            "ModifyExpressionValue", "ModifyReturnValue", "WrapOperation", "WrapWithCondition", "WrapMethod")


def java_files(dirs):
    for d in dirs:
        for root, _, files in os.walk(d):
            for f in files:
                if f.endswith(".java") and "/mixin/" in os.path.join(root, f).replace(os.sep, "/"):
                    yield os.path.join(root, f)


def balanced(text, start):
    """The text inside the parentheses that open at text[start]."""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
    return text[start + 1:]


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", lambda m: " " * len(m.group(0)), src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def parse_mixin(path):
    src = strip_comments(open(path, encoding="utf-8").read())
    imports = {m.group(2): m.group(1) + "." + m.group(2) for m in re.finditer(r"^import\s+([\w.]+)\.(\w+);", src, re.M)}
    m = re.search(r"@Mixin\s*\(", src)
    if not m:
        return None
    body = balanced(src, m.end() - 1)
    targets = []
    for c in re.findall(r"([\w.]+)\.class", body):
        parts = c.split(".")
        if parts[0] in imports:
            targets.append(imports[parts[0]] + "".join("$" + p for p in parts[1:]))
        elif c.startswith("net."):
            targets.append(c)
    for t in re.findall(r'"([\w./$]+)"', body):
        targets.append(t.replace("/", "."))
    members = []  # (kind, annotation text, member name)
    for a in re.finditer(r"@(\w+)", src[m.end():]):
        kind = a.group(1)
        if kind not in HANDLERS + ("Overwrite", "Shadow", "Accessor", "Invoker"):
            continue
        pos = m.end() + a.end()
        args = ""
        rest = src[pos:]
        if rest.lstrip().startswith("("):
            open_at = pos + (len(rest) - len(rest.lstrip()))
            args = balanced(src, open_at)
            pos = open_at + len(args) + 2
        # The member the annotation sits on: the next declaration's name.
        decl = re.match(r"(?:\s*@\w+(?:\([^)]*\))?)*\s*([^;{=(]*?)\b(\w+)\s*(\(|;|=)", src[pos:], re.S)
        name = decl.group(2) if decl else "?"
        is_method = bool(decl and decl.group(3) == "(")
        members.append((kind, args, name, is_method))
    return targets, members


def selectors(args):
    m = re.search(r"\bmethod\s*=\s*(\{[^}]*\}|\"[^\"]*\")", args)
    if not m:
        return []
    return re.findall(r'"([^"]*)"', m.group(1))


def selector_name(sel):
    sel = re.sub(r"^L[\w/$]+;", "", sel)
    return sel.split("(")[0].split(":")[0].rstrip("*")


def at_targets(args):
    return re.findall(r'\btarget\s*=\s*"([^"]+)"', args)


def ref_of(target):
    """A mixin target string as it shows in api-calls.py's summaries (owner.name:desc), or None."""
    m = re.match(r"^L([\w/$]+);([\w<>$]+)(\(.*)$", target)
    if m:
        name = '"%s"' % m.group(2) if m.group(2).startswith("<") else m.group(2)
        return "%s.%s:%s" % (m.group(1), name, m.group(3))
    m = re.match(r"^L([\w/$]+);([\w$]+):(.+)$", target)
    if m:
        return "%s.%s:%s" % (m.group(1), m.group(2), m.group(3))
    return None


def load_members(path):
    """class -> {member name -> [javap lines]}, from javap -p -s output."""
    classes = collections.defaultdict(lambda: collections.defaultdict(list))
    cls = None
    simple = None
    for line in open(path, errors="replace"):
        line = line.rstrip("\n")
        m = re.match(r"^(?:[\w ]+ )?(?:class|interface|enum|record) ([\w.$]+)", line)
        if m and not line.startswith(" "):
            cls = m.group(1)
            simple = cls.split(".")[-1].split("$")[-1]
            classes[cls]
            continue
        if cls and line.startswith("  ") and not line.strip().startswith("descriptor:"):
            s = line.strip()
            mm = re.search(r"([\w$<>]+)\(", s)
            if mm:
                name = mm.group(1)
                if name.split(".")[-1].split("$")[-1] == simple:
                    name = "<init>"
                if s.startswith("static {}"):
                    name = "<clinit>"
            else:
                mf = re.search(r"([\w$]+);$", s)
                name = mf.group(1) if mf else None
            if name:
                classes[cls][name].append(s)
    return classes


def load_calls(path):
    """class -> {method name -> sorted ref lines (all overloads)}, from api-calls.py output."""
    calls = collections.defaultdict(lambda: collections.defaultdict(list))
    cls = None
    method = None
    for line in open(path, errors="replace"):
        line = line.rstrip("\n")
        if line and not line.startswith(" "):
            cls = line.strip()
            method = None
            continue
        if line.startswith("  ") and not line.startswith("    "):
            s = line.strip()
            mm = re.search(r"([\w$<>]+)\(", s)
            simple = cls.split(".")[-1].split("$")[-1] if cls else ""
            method = mm.group(1) if mm else s
            if method.split(".")[-1].split("$")[-1] == simple:
                method = "<init>"
            if s.startswith("static {}"):
                method = "<clinit>"
            calls[cls][method].append("  " + s)
            continue
        if line.startswith("    ") and cls and method:
            calls[cls][method].append(line)
    return calls


def count_of(lines, ref):
    n = 0
    for l in lines:
        if ref in l:
            m = re.search(r" x(\d+)$", l)
            n += int(m.group(1)) if m else 1
    return n


def main():
    if sys.argv[1] == "targets":
        found = set()
        for path in java_files(sys.argv[2:]):
            parsed = parse_mixin(path)
            if parsed:
                found.update(parsed[0])
        print("\n".join(sorted(found)))
        return
    old_dir, new_dir = sys.argv[2], sys.argv[3]
    old_members, new_members = load_members(old_dir + "/members.txt"), load_members(new_dir + "/members.txt")
    old_calls, new_calls = load_calls(old_dir + "/calls.txt"), load_calls(new_dir + "/calls.txt")
    problems = 0
    for path in sorted(java_files(sys.argv[4:])):
        parsed = parse_mixin(path)
        if not parsed:
            continue
        targets, members = parsed
        report = []
        for target in targets:
            if target not in new_members:
                report.append("TARGET CLASS MISSING in new: " + target)
                continue
            nm, om = new_members[target], old_members.get(target, {})
            nc, oc = new_calls.get(target, {}), old_calls.get(target, {})
            seen_methods = set()
            for kind, args, name, is_method in members:
                if kind == "Shadow" or kind == "Overwrite":
                    want = [name]
                elif kind == "Accessor":
                    m = re.search(r'"([^"]+)"', args)
                    want = [m.group(1) if m else re.sub(r"^(get|is|set)", "", name)[:1].lower() + re.sub(r"^(get|is|set)", "", name)[1:]]
                elif kind == "Invoker":
                    m = re.search(r'"([^"]+)"', args)
                    want = [m.group(1) if m else re.sub(r"^(call|invoke)", "", name)[:1].lower() + re.sub(r"^(call|invoke)", "", name)[1:]]
                else:
                    want = [selector_name(s) for s in selectors(args)]
                for w in want:
                    if not w or "*" in w:
                        continue
                    if w not in nm:
                        if kind == "Shadow" and w not in om:
                            continue  # shadows an inherited member: the compiler checks those
                        report.append("%s %s: %s.%s MISSING in new%s" % ("@" + kind, name, target, w,
                                                                        "" if w in om else " (not in old either)"))
                        continue
                    if kind in HANDLERS or kind == "Overwrite":
                        for t in at_targets(args):
                            ref = ref_of(t)
                            if not ref:
                                continue
                            n_new, n_old = count_of(nc.get(w, []), ref), count_of(oc.get(w, []), ref)
                            if n_new == 0:
                                report.append("@%s %s: @At target %s not called in new %s.%s (old: %d)" % (kind, name, t, target, w, n_old))
                            elif n_new != n_old:
                                report.append("@%s %s: @At target %s called %d times in new %s.%s, %d in old (ordinals)" % (kind, name, t, n_new, target, w, n_old))
                        if (target, w) not in seen_methods and nc.get(w, []) != oc.get(w, []):
                            seen_methods.add((target, w))
                            old_set, new_set = set(oc.get(w, [])), set(nc.get(w, []))
                            lines = ["-" + l for l in oc.get(w, []) if l not in new_set] + ["+" + l for l in nc.get(w, []) if l not in old_set]
                            report.append("@%s: %s.%s changed:\n%s" % (kind, target, w, "\n".join("      " + l for l in lines[:40])
                                                                       + ("\n      ... %d more" % (len(lines) - 40) if len(lines) > 40 else "")))
                    if kind in ("Shadow", "Overwrite") and nm.get(w) != om.get(w) and w in om:
                        report.append("@%s %s: signature of %s.%s changed: %s -> %s" % (kind, name, target, w, om.get(w), nm.get(w)))
        if report:
            problems += 1
            print("== " + path)
            for r in report:
                print("  " + r)
    print("%d mixins to look at" % problems)


if __name__ == "__main__":
    main()
