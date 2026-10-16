"""Summarizes `javap -c -p` output for api-probe's [api-calls]: for each method, the methods it calls, the fields it reads
and writes and the constants it loads, each with a count, sorted. That is what a mixin's @At targets (INVOKE, FIELD,
CONSTANT and their ordinals) need to be checked against; the code itself (its order and control flow) is left out.

python3 api-calls.py <javap -c -p output>
"""
import collections
import re
import sys

CONSTS = {"iconst_m1": ("int", "-1"), "lconst_0": ("long", "0"), "lconst_1": ("long", "1"),
          "fconst_0": ("float", "0.0"), "fconst_1": ("float", "1.0"), "fconst_2": ("float", "2.0"),
          "dconst_0": ("double", "0.0"), "dconst_1": ("double", "1.0"), "aconst_null": ("null", "null")}


def main(path):
    out = sys.stdout
    cls = None
    method = None
    refs = None

    def flush():
        if method is not None and refs:
            out.write("  " + method + "\n")
            for (kind, text), n in sorted(refs.items()):
                out.write(f"    {kind} {text}" + (f" x{n}" if n > 1 else "") + "\n")

    for line in open(path, errors="replace"):
        line = line.rstrip("\n")
        if line.startswith("Compiled from"):
            continue
        m = re.match(r'^(?:[\w ]+ )?(?:class|interface|enum|record) ([\w.$]+)', line)
        if m and not line.startswith(" "):
            flush()
            method = None
            cls = m.group(1)
            out.write(cls + "\n")
            continue
        if line.startswith("  ") and not line.startswith("   ") and line.endswith((";", ");", "};")):
            flush()
            method = line.strip()
            refs = collections.Counter()
            continue
        if refs is None or not line.startswith("      "):
            continue
        m = re.match(r'^\s+\d+: (\w+)(.*)$', line)
        if not m:
            continue
        op, rest = m.group(1), m.group(2)
        comment = rest.split("//", 1)[1].strip() if "//" in rest else ""
        if op.startswith("invoke") and op != "invokedynamic":
            target = re.sub(r'^(Method|InterfaceMethod) ', '', comment)
            if "." not in target.split(":")[0]:
                target = cls.replace(".", "/") + "." + target
            refs[("I", target)] += 1
        elif op == "invokedynamic":
            refs[("D", re.sub(r'^InvokeDynamic #\d+:', '', comment))] += 1
        elif op in ("getfield", "putfield", "getstatic", "putstatic"):
            target = re.sub(r'^Field ', '', comment)
            if "." not in target.split(":")[0]:
                target = cls.replace(".", "/") + "." + target
            refs[("F" + ("w" if op.startswith("put") else "r"), target)] += 1
        elif op in ("ldc", "ldc_w", "ldc2_w"):
            m2 = re.match(r'^(int|float|long|double|String|class) (.*)$', comment)
            if m2:
                kind, value = m2.groups()
                if kind == "String" and len(value) > 60:
                    value = value[:60] + "..."
                refs[("C", f"{kind} {value}")] += 1
        elif op in ("bipush", "sipush"):
            refs[("C", "int " + rest.strip().split()[0])] += 1
        elif re.match(r'^iconst_\d$', op):
            refs[("C", "int " + op[-1])] += 1
        elif op in CONSTS:
            kind, value = CONSTS[op]
            refs[("C", f"{kind} {value}")] += 1
        elif op in ("new", "checkcast", "instanceof", "anewarray"):
            refs[("T" + op[0], re.sub(r'^class ', '', comment))] += 1
    flush()


main(sys.argv[1])
