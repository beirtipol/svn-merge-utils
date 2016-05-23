<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema">

	<xsl:param name="baseUrl" />

	<xsl:template match="/">
		<html>
			<head>
				<style>
					h2 {
						margin-bottom: 50px;
					}
					
					.container {
						text-align: center;
						overflow: 	hidden;
						width: 		800px;
						margin: 	0 auto;
					}
					
					.container td, .container th {
						padding: 		10px;
						white-space: 	pre;
					}
					
					.container td:first-child, .container th:first-child {
						padding-left:	20px;
					}
					
					.container td:last-child, .container th:last-child {
						padding-right: 	20px;
					}
					
					.container th {
						border-bottom: 	1px solid #ddd;
						position:		relative;
					}
				</style>
				<link href="http://netdna.bootstrapcdn.com/twitter-bottstrap/2.2.2/css/bootstrap.no-icons.min.css" rel="stylesheet"/>
				
				<script>
					(function(document) {
						'use strict';
						
						var LightTableFilter = (function(Arr) {
							var _input;
							
							function _onInputEvent(e) {
								_input = e.target;
								var tables = document.getElementsByClassName(_input.getAttribute('data-table'));
								Arr.forEach.call(tables, function(table){
									Arr.forEach.call(table.tBodies, function(tbody){
										Arr.forEach.call(tbody.rows, _filter);
									});
								});
							}
							
							function _filter(row) {
								var text = row.textContent.toLowerCase(), val = _input.value.toLowerCase();
								row.style.display = text.indexOf(val) === -1 ? 'none' : 'table-row';
							}
							
							return {
								init: function(){
									var inputs = document.getElementsByClassName('light-table-filter');
									Arr.forEach.call(inputs, function(input) {
										input.oninput = _onInputEvent;
									});
								}
							};
						})(Array.prototype);
						
						document.addEventListener('readystatechange', function() {
							if(document.readyState === 'complete'){
								LightTableFilter.init();
							}
						});
					})(document);
				</script>
			</head>
			<body>
				<section class="container">
					<h3>
						<xsl:text>Missing Merge Report - </xsl:text>
						<xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>
					</h3>
					<h4>
						<xsl:value-of select="$baseUrl" />
					</h4>
					<input type="search" class="light-table-filter" data-table="logentry-table" placeholder="Filter"/>
					<table class="logentry-table">
						<thead>
							<tr>
								<th>Revision</th>
								<th>Source</th>
								<th>Target</th>
								<th>Date</th>
								<th>Author</th>
								<th>Message</th>
								<th>Paths</th>
							</tr>
						</thead>
						<tbody>
							<xsl:for-each select="log/logentry">
								<tr>
									<td>
										<xsl:value-of select="@revision" />
									</td>
									<td>
										<xsl:value-of select="@mergeSource" />
									</td>
									<td>
										<xsl:value-of select="@mergeTarget" />
									</td>
									<td>
										<xsl:variable name="dt" select="date" />
										<xsl:value-of select="format-dateTime(xs:dateTime($dt), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>
									</td>
									<td>
										<xsl:value-of select="author" />
									</td>
									<td>
										<pre>
											<xsl:value-of select="msg" />
										</pre>
									</td>
									<td>
										<pre>
											<xsl:for-each select="paths/path">
												<xsl:value-of select="@action" />
												<xsl:text> </xsl:text>
												<xsl:value-of select="." />
												<xsl:text>&#xa;</xsl:text>
											</xsl:for-each>
										</pre>
									</td>
								</tr>
							</xsl:for-each>
						</tbody>
					</table>
				</section>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>