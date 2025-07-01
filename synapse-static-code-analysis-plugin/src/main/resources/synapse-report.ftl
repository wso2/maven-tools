<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Static Code Analysis Report</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      margin: 20px;
      background: #f9f9f9;
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    header {
      background-color: #1f2937;
      color: white;
      padding: 1rem;
      border-radius: 8px;
    }
    section {
      margin-top: 2rem;
    }
    .cards {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .card {
      background: white;
      border-radius: 8px;
      padding: 1rem;
      flex: 1;
      min-width: 180px;
      box-shadow: 0 0 5px rgba(0,0,0,0.1);
    }
    .syntax-ok { background: #10b981; color: white; }
    .syntax-error { background: #ef4444; color: white; }
    .metric { background: #3b82f6; color: white; }
    .rule-critical { background: #ef4444; color: white; }
    .rule-major { background: #f97316; color: white; }
    .rule-minor { background: #facc15; }
    table {
      margin-top: 1rem;
      width: 100%;
      border-collapse: collapse;
      background: white;
    }
    table th, table td {
      border: 1px solid #e5e7eb;
      padding: 0.5rem;
      text-align: left;
    }
    table th {
      background: #f3f4f6;
    }
    footer {
      margin-top: auto;
      background-color: #1f2937;
      color: white;
      padding: 1rem;
      text-align: center;
      border-radius: 8px;
      margin-top: 2rem;
    }
    footer a {
      color: #60a5fa;
      text-decoration: none;
    }
    footer a:hover {
      text-decoration: underline;
    }
  </style>
</head>
<body>
  <header>
    <h1>Static Code Analysis Report</h1>
    <p>Project: ${projectName} | Generated At: ${generatedAt} | Analyzer: Synapse Static Code Analyzer</p>
  </header>

<!-- Section 1: Syntax Validation -->
<section>
  <h2>Syntax Validation</h2>

  <#if (syntaxIssues?size > 0)>
  <!-- Status summary -->
  <div class="cards">
    <div class="card syntax-error">Status: Failed (${syntaxIssues?size} errors)</div>
  </div>

  <!-- Syntax errors list -->
  <table>
    <thead>
      <tr>
        <th>File</th>
        <th>Line</th>
        <th>Severity</th>
        <th>Message</th>
      </tr>
    </thead>
    <tbody>
    <#list syntaxIssues as issue>
      <tr>
        <td>${issue.filePath}</td>
        <td>${issue.range.start.line + 1}</td>
        <td style="color:red; font-weight:bold;">${issue.severity}</td>
        <td>${issue.message}</td>
      </tr>
    </#list>
    </tbody>
  </table>
  <#else>
  <div class="card syntax-ok">Status: Passed</div>
  <p>No syntax errors found.</p>
  </#if>
</section>
</body>
</html>
