param([string]$BaseUrl = 'http://localhost:18091')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
# Run against a test-profile server. This adds one new member and deletes nothing.
$handler = New-Object System.Net.Http.HttpClientHandler
$handler.UseProxy = $false
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(15)

function Assert-ApiResponse([string]$Method, [string]$Body, [int]$ExpectedStatus, [string]$ExpectedCode) {
    $request = New-Object System.Net.Http.HttpRequestMessage
    $request.Method = New-Object System.Net.Http.HttpMethod($Method)
    $request.RequestUri = [Uri]("$BaseUrl/api/v1/members")
    if ($null -ne $Body -and $Method -eq 'POST') {
        $request.Content = New-Object System.Net.Http.StringContent($Body, [Text.Encoding]::UTF8, 'application/json')
    }
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $json = $raw | ConvertFrom-Json
        if ([int]$response.StatusCode -ne $ExpectedStatus -or $json.statusCode -ne $ExpectedStatus) {
            throw "Unexpected HTTP/body status: $([int]$response.StatusCode)/$($json.statusCode)"
        }
        if ($json.resultCode -ne $ExpectedCode) { throw "Unexpected resultCode: $($json.resultCode)" }
        $keys = @($json.PSObject.Properties.Name | Sort-Object)
        if (($keys -join ',') -ne 'data,msg,resultCode,statusCode') { throw 'Invalid response envelope' }
        if ($raw.Contains('"password"') -or $raw.Contains('example-only')) { throw 'Password exposed' }
        Write-Output ("PASS {0} {1} {2}" -f $Method, $ExpectedStatus, $ExpectedCode)
        if ($ExpectedStatus -eq 201) {
            if ($json.data.item.username -ne $script:username) { throw 'Incorrect request mapping' }
            Write-Output ("CREATED memberId={0} username={1}" -f $json.data.item.id, $json.data.item.username)
        }
    } finally {
        if ($null -ne $response) { $response.Dispose() }
        $request.Dispose()
    }
}

try {
    $script:username = 'ps-' + [Guid]::NewGuid().ToString('N')
    $body = @{username=$script:username; password='example-only'; nickname='API example'} | ConvertTo-Json -Compress
    Assert-ApiResponse 'POST' $body 201 '201-1'
    Assert-ApiResponse 'POST' $body 400 '400-1'
    $missing = @{username='missing-' + [Guid]::NewGuid().ToString('N'); password='example-only'} | ConvertTo-Json -Compress
    Assert-ApiResponse 'POST' $missing 400 '400-NotBlank'
    Assert-ApiResponse 'POST' '{' 400 '400-JSON'
    Assert-ApiResponse 'GET' $null 405 '405-1'
    Write-Output 'ALL 5 HTTP CASES PASSED'
} finally {
    $client.Dispose()
    $handler.Dispose()
}
