import test from 'node:test';
import assert from 'node:assert/strict';
import { transactionStorageKey, prepareTransaction, loadTransaction, clearTransaction, terminalTransaction } from '../src/views/member/orderuser/components/transactionRequest.js';
function storage() { const map = new Map(); return { getItem: k => map.get(k) ?? null, setItem: (k,v) => map.set(k,v), removeItem: k => map.delete(k) }; }
const payload = { userId: 213, amount: 405, operationType: 0 };
test('timeout and refresh preserve exact original intent even if form changes', () => {
 const s=storage(), key=transactionStorageKey('api', 1,213);
 const first=prepareTransaction(s,key,payload);
 assert.equal(first.requestId.length,32);
 assert.deepEqual(loadTransaction(s,key),first);
 assert.deepEqual(prepareTransaction(s,key,{...payload,amount:810}),first);
});
test('unknown or mismatched result cannot confirm an intent', () => {
 assert.equal(terminalTransaction({requestId:'a',state:'UNKNOWN'},'a'),false);
 assert.equal(terminalTransaction({requestId:'b',state:'SUCCEEDED'},'a'),false);
 assert.equal(terminalTransaction({requestId:'a',state:'SUCCEEDED'},'a'),true);
 assert.equal(terminalTransaction({requestId:'a',state:'FAILED'},'a'),true);
});
test('late response cannot erase a newer intent; only terminal matching intent can clear', () => {
 const s=storage(), key='key'; const first=prepareTransaction(s,key,payload);
 clearTransaction(s,key,'other'); assert.deepEqual(loadTransaction(s,key),first);
 clearTransaction(s,key,first.requestId); assert.equal(loadTransaction(s,key),null);
 const next=prepareTransaction(s,key,payload); assert.notEqual(first.requestId,next.requestId);
 clearTransaction(s,key,first.requestId); assert.deepEqual(loadTransaction(s,key),next);
});
test('storage failure or corrupted journal fails closed before a new request exists', () => {
 assert.throws(()=>prepareTransaction({getItem:()=>null,setItem:()=>{throw Error('full');}},'k',payload),/full/);
 assert.throws(()=>loadTransaction({getItem:()=>'{invalid'},'k'));
 assert.throws(()=>transactionStorageKey('api','',213));
 assert.notEqual(transactionStorageKey('api',1,213),transactionStorageKey('api',2,213));
});
