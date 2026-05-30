import glob
for file in glob.glob("src/main/resources/static/*.html"):
    print(f"--- {file} ---")
    with open(file, 'r') as f:
        content = f.read()
        import re
        m = re.search(r'<style>(.*?)</style>', content, re.DOTALL)
        if m:
            print(m.group(1).strip()[:100] + "...")
