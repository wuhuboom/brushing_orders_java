const fs = require('fs');
const path = require('path');

function check(file){
  const text = fs.readFileSync(file,'utf8');
  console.log(`Reading ${file}, length: ${text.length}`);
  console.log(`Last 20 chars: ${JSON.stringify(text.slice(-20))}`);
  const openCount = (text.match(/\{/g)||[]).length;
  const closeCount = (text.match(/\}/g)||[]).length;
  console.log(`${file}: '{'=${openCount}, '}'=${closeCount}`);
  let line=1; let col=0; let stack=[];
  for(let i=0;i<text.length;i++){
    const ch=text[i];
    if(ch==='\n'){line++;col=0;continue;} col++;
    if(ch==='{') stack.push({line,col});
    if(ch==='}'){
      if(stack.length===0){
        console.log(`${file}: Unmatched closing brace at ${line}:${col}`);
      } else stack.pop();
    }
  }
  if(stack.length) console.log(`${file}: ${stack.length} unmatched opening brace(s). In stack: ${JSON.stringify(stack)}`);
  else console.log(`${file}: Braces balanced`);
}

const files=['src/locales/zh-CN.js','src/locales/en-US.js'];
files.forEach(f=>{
  const p=path.resolve(__dirname,'..',f);
  if(fs.existsSync(p)) check(p);
  else console.log(`${p} not found`);
});
