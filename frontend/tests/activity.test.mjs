import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";
const require = createRequire(import.meta.url); const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
function load(file, mocks = {}) { const source = fs.readFileSync(path.join(root,file),"utf8"); const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020,jsx:ts.JsxEmit.ReactJSX}}).outputText; const mod={exports:{}}; vm.runInNewContext(js,{module:mod,exports:mod.exports,require:(name)=>name in mocks?mocks[name]:name==="@/common/format/money"?{formatCount:(v,u=String.fromCharCode(0xAC74))=>`${Number(v).toLocaleString("ko-KR")}${u}`} : require(name),URLSearchParams}); return mod.exports; }
const plain=(x)=>JSON.parse(JSON.stringify(x)); const builder={query:(x)=>x,mutation:(x)=>x}; const baseApi={injectEndpoints:({endpoints})=>endpoints(builder)};

test("activity API matches controller query contract and exposes paged response fields",()=>{
  const api=load("activity/controller/activityApi.ts",{"@/common/api/baseApi":{baseApi}}).activityApi;
  const query=api.getMoneyBookActivities.query({moneyBookUid:8,startDate:"2026-10-01",endDate:"2026-10-02",actorUserUid:4,activityType:"TRANSACTION_CREATED",targetType:"TRANSACTION",page:2,size:50});
  assert.deepEqual(plain(query),{url:"money-books/8/activities",params:{startDate:"2026-10-01",endDate:"2026-10-02",actorUserUid:4,activityType:"TRANSACTION_CREATED",targetType:"TRANSACTION",page:2,size:50}});
  assert.deepEqual(plain(api.getMoneyBookActivities.providesTags({},undefined,{moneyBookUid:8})),[{type:"MoneyBookActivity",id:8}]);
});

test("activity page renders snapshot actor and summary and keeps loading, error, empty, and R permission states",()=>{
  const React=require("react"); const {renderToStaticMarkup}=require("react-dom/server");
  const todaySeoul=new Intl.DateTimeFormat("sv-SE",{timeZone:"Asia/Seoul",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());
  const sample={activityUid:1,actorUserUid:3,actorNickname:"과거 이름",activityType:"TRANSACTION_CREATED",targetType:"TRANSACTION",targetUid:4,summary:"거래를 등록했습니다.",metadataJson:null,occurredAt:`${todaySeoul}T14:31:00`};
  function render({canRead=true,result={content:[sample],page:0,size:20,totalElements:1,totalPages:1,first:true,last:true},isLoading=false,isError=false}={}) {
    const {default:View}=load("activity/components/MoneyBookActivityView.tsx",{
      "react":React,
      "@/moneybook/hooks/useMoneyBookPermission":{useMoneyBookPermission:()=>({canRead,isLoading:false})},
      "@/moneybook/hooks/useMoneyBookMembers":{useMoneyBookMembers:()=>({members:[]})},
      "../activityLabels":load("activity/activityLabels.ts"),
      "../hooks/useMoneyBookActivities":{useMoneyBookActivities:()=>({filters:{startDate:"",endDate:"",actorUserUid:"",activityType:"",targetType:"",page:0,size:20},validRange:true,update(){},reset(){},setPage(){},result,isLoading,isFetching:false,isError,errorMessage:isError?"활동내역을 불러오지 못했습니다.":null,refetch(){}})},
    });
    return renderToStaticMarkup(React.createElement(View,{moneyBookUid:5}));
  }
  let markup=render(); assert.match(markup,/과거 이름/); assert.match(markup,/거래를 등록했습니다/); assert.match(markup,/거래 등록/); assert.match(markup,/오늘/);
  assert.match(render({result:{content:[],page:0,size:20,totalElements:0,totalPages:0,first:true,last:true}}),/아직 기록된 활동이 없습니다/);
  assert.match(render({isLoading:true,result:undefined}),/불러오는 중/);
  assert.match(render({isError:true,result:undefined}),/불러오지 못했습니다/);
  assert.match(render({canRead:false}),/조회 권한이 없습니다/);
});

test("activity labels cover every backend enum and unknown values fall back to their raw value",()=>{
  const labels=load("activity/activityLabels.ts");
  assert.equal(labels.activityTypes.length,30); assert.equal(Object.keys(labels.activityTypeLabels).length,labels.activityTypes.length);
  assert.equal(labels.targetTypes.length,12); assert.equal(Object.keys(labels.targetTypeLabels).length,labels.targetTypes.length);
  assert.equal(labels.getActivityTypeLabel("TRANSACTION_CREATED"),"거래 등록"); assert.equal(labels.getActivityTypeLabel("FUTURE_EVENT"),"FUTURE_EVENT");
  assert.equal(labels.getTargetTypeLabel("RECURRING_TRANSACTION"),"정기 수입/지출");
});

test("activity dates preserve backend LocalDateTime as Korea time and metadata exposes only allowlisted hints",()=>{
  const labels=load("activity/activityLabels.ts");
  const time=labels.formatActivityTime("2026-10-02T14:31:00"); assert.equal(time.dateKey,"2026-10-02"); assert.match(time.time,/2:31/);
  assert.equal(labels.getActivityMetadataHint("RECURRING_GENERATED",'{"generatedCount":5,"private":"x"}'),"생성 5건");
  assert.equal(labels.getActivityMetadataHint("MONTH_CLOSED",'{"year":2026,"month":10}'),"2026년 10월");
  assert.equal(labels.getActivityMetadataHint("MEMBER_PERMISSION_UPDATED",'{"secret":"hidden"}'),null);
});

test("activity URL hook parses filters, uses page/size defaults, resets page on filters and reset clears query",()=>{
  const calls=[]; const router={push:(url)=>calls.push(url)}; const params=new URLSearchParams("startDate=2026-10-01&activityType=TRANSACTION_CREATED&page=3&size=50");
  const apiHook=()=>({currentData:null,isFetching:false,isError:false});
  const {useMoneyBookActivities}=load("activity/hooks/useMoneyBookActivities.ts",{
    "next/navigation":{useSearchParams:()=>params,useRouter:()=>router,usePathname:()=>"/books/8/activities"},
    "@/common/api/getApiErrorMessage":{getApiErrorMessage:()=>"err"},"../controller/activityApi":{useGetMoneyBookActivitiesQuery:apiHook},"../activityLabels":{activityTypes:["TRANSACTION_CREATED"],targetTypes:["TRANSACTION"]},
  });
  const view=useMoneyBookActivities(8,true); assert.equal(view.filters.page,3); assert.equal(view.filters.size,50); assert.equal(view.filters.activityType,"TRANSACTION_CREATED");
  view.update("targetType","TRANSACTION"); assert.equal(calls[0],"/books/8/activities?startDate=2026-10-01&activityType=TRANSACTION_CREATED&size=50&targetType=TRANSACTION");
  view.reset(); assert.equal(calls[1],"/books/8/activities");
});
