## ReAI accounting system

[ReAI](https://reai.no) is an "AI first" accounting system and ERP. 

Some of the features: 

* Bill payment via bank integrations
* Invoicing: outgoing invoices, credit notes, order confirmation, offers etc. 
* Product and inventory management
* Book-keeping, credits and debits
* Voucher reception, company email to receive invoices, receipts etc. 
* Bank reconciliation, automated via bank-feeds
* Integration with tax authorities: VAT, annual accounts, salary 
* Timesheets, for automating salary payments
* Reports: Balance sheet, profit/loss, trial balance etc.
* Expenses
* Projects 
* Chart of accounts 
* Subsidiary ledgers: Employee, supplier, customer

And finally agentic AI for automating all of the above. 

## Project conventions: AI and developer instructions  

We use a shared guidelines in [AI.md](AI.md), these are guidelines for developers and AI agents.

Symlink the AI instructions to give these instructions to your preferred agent: 

```shell
# Leading agents will change over time
ln AI.md CLAUDE.md # Claude code : https://www.anthropic.com/engineering/claude-code-best-practices
ln AI.md AGENTS.md # Codex (OpenAI) : https://platform.openai.com/docs/codex/overview
ln AI.md GEMINI.md # Gemini CLI
ln AI.md .junie/guidelines.md # Jetbrains Junie
```

If using a browser UI for AI, you can make a project and include it in "project instructions"

## Frontend autocompletion 

`npm install` -> [dist-cdn](node_modules/%40awesome.me/webawesome/dist-cdn) 
which gives frontend auto-completion and CMD+click for Web awesome classes. 
You can remove the duplicate dist folder to avoid duplicate auto-completion alternatives.  
