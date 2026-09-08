param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$cases = @(
    @{ Question = 'Show revenue for the last 12 months.'; Function = 'getRevenueByPeriod' },
    @{ Question = 'Compare India and Germany sales.'; Function = 'compareCountries' },
    @{ Question = 'Show the top 10 customers.'; Function = 'getTopCustomers' },
    @{ Question = 'Which products are declining?'; Function = 'getDecliningProducts' },
    @{ Question = 'Why did revenue change last quarter?'; Function = 'comparePeriods' }
)
foreach ($case in $cases) {
    $body = @{ question = $case.Question } | ConvertTo-Json
    $result = Invoke-RestMethod -Uri "$BaseUrl/api/chat" -Method Post -ContentType 'application/json' -Body $body
    if ($result.function -ne $case.Function -or $result.table.Count -eq 0) {
        throw "Unexpected response for $($case.Question)"
    }
    Write-Output "PASS: $($case.Question) => $($result.answer)"
}
