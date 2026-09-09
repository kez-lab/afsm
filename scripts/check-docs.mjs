// Run with node scripts/check-docs.mjs. No browser or package installation needed.
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import assert from 'node:assert/strict';
const element = () => ({dataset: {}, classList: {add(){},remove(){},toggle(){}},
  addEventListener(){}, setAttribute(){}, querySelector(){return element();},
  querySelectorAll(){return [];}, append(){}, replaceChildren(){}, scrollIntoView(){}});
const context = {window:{}, document:{documentElement:{dataset:{language:'ko'}},
  querySelector:element, querySelectorAll:()=>[], createElement:element}, setTimeout(){}};
runInNewContext(readFileSync('docs/js/trace-lab.js','utf8'),context);
// Language selection resets the lab before the app selects its initial example.
assert.doesNotThrow(() => context.window.afsmTraceLab.resetExampleLab());
for (const key of ['draft','auth','checkout','product-editor']) {
  assert.doesNotThrow(() => context.window.afsmTraceLab.selectExample(key, false));
}
console.log('PASS: initial language reset and all four example initializations');
