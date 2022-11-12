#encoding:utf-8
def f1():
    from re import search
    def tiddler(f, txt, tag):
        f.write('{"created":"20221112150542736","text":"","tags":"[[' + tag + ']]","title":"'+txt+'","modified":"20221112161845737"},\n')
    src_lines = open("1.txt", encoding = "utf-8").readlines()
    lines = []
    
    for lx in range(len(src_lines)):
        lines.append((len(search("^\t*", src_lines[lx]).group(0)), 'BJNR010050934.'+(f"{lx + 1:0>3}. ") + src_lines[lx].strip()))
    
    f = open("2.txt", encoding = "utf-8", mode = "w")
    stack = ["Einkommensteuergesetz_19.10.2022"]
    tiddler(f, lines[0][1], stack[-1])
    for lx in range(1, len(lines)):
        if lines[lx][0] > lines[lx - 1][0]:
            stack.append(lines[lx - 1][1])
        elif lines[lx][0] < lines[lx - 1][0]:
            for i in range(lines[lx - 1][0] - lines[lx][0]):
                stack.pop()
        tiddler(f, lines[lx][1], stack[-1])
    f.close()
f1()