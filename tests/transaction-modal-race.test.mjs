import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { terminalTransaction, clearTransaction } from '../src/views/member/orderuser/components/transactionRequest.js';
const source = readFileSync(new URL('../src/views/member/orderuser/components/OrderuserTransactionModal.vue', import.meta.url), 'utf8');
const code = source.slice(source.indexOf('function acceptResult('), source.indexOf('async function handleConfirm('));
function harness(current = 'new-request', generation = 1) {
 const map = new Map([['key', JSON.stringify({requestId:current,userId:213})]]);
 const events=[];
 const context={ terminalTransaction,clearTransaction,generation,pending:{value:{requestId:current}},
 localStorage:{getItem:k=>map.get(k)??null,removeItem:k=>map.delete(k)},
 stopPolling:()=>events.push('stop'),emit:e=>events.push(e),visible:{value:true},
 proxy:{$modal:{msgError:m=>events.push(m),msgSuccess:m=>events.push(m)}},
 journalKey:'key',pollTimer:null,setTimeout:()=>events.push('timer') };
 vm.createContext(context); vm.runInContext(code,context);
 return {context,map,events};
}
test('late terminal response must preserve newer pending UI and storage',()=>{
 for(const state of ['FAILED','SUCCEEDED']) {
  const {context:c,map,events}=harness();
  assert.equal(c.acceptResult({requestId:'old-request',state},{requestId:'old-request'},'key',1),true);
  assert.equal(c.pending.value.requestId,'new-request');
  assert.equal(JSON.parse(map.get('key')).requestId,'new-request');
  assert.equal(c.visible.value,true); assert.deepEqual(events,[]);
 }
});
test('duplicate terminal response must not emit success twice',()=>{
 const {context:c,events}=harness('same-request');
 const result={requestId:'same-request',state:'SUCCEEDED'};
 c.acceptResult(result,result,'key',1); c.acceptResult(result,result,'key',1);
 assert.equal(events.filter(x=>x==='success').length,1);
});
test('old polling response must not start a polling loop for a newer intent',async()=>{
 const {context:c,events}=harness('old-request');
 let resolve; c.transactionStatus=()=>new Promise(r=>{resolve=r;});
 const run=c.pollResult(1);
 c.pending.value={requestId:'new-request'};
 resolve({data:{requestId:'old-request',state:'UNKNOWN'}}); await run;
 assert.deepEqual(events,[]);
});
test('terminal response from closed generation does not mutate current UI',()=>{
 const {context:c,events}=harness();
 c.acceptResult({requestId:'old-request',state:'SUCCEEDED'},{requestId:'old-request'},'key',0);
 assert.equal(c.pending.value.requestId,'new-request'); assert.deepEqual(events,[]);
});
