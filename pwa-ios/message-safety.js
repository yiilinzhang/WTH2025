// Message Safety Module - Prevents scams by detecting sensitive information
// This module screens messages before sending to protect users from sharing sensitive data

class MessageSafety {
    constructor() {
        // Patterns for detecting sensitive information
        this.sensitivePatterns = {
            creditCard: {
                pattern: /\b(?:\d{4}[\s-]?){3}\d{4}\b/,
                message: 'Credit card number detected',
                severity: 'high'
            },
            ssn: {
                pattern: /\b\d{3}-\d{2}-\d{4}\b/,
                message: 'Social Security Number detected',
                severity: 'high'
            },
            bankAccount: {
                pattern: /\b\d{8,17}\b/,
                message: 'Possible bank account number',
                severity: 'medium'
            },
            phone: {
                pattern: /(?:\+?\d{1,3}[\s-]?)?\(?\d{3}\)?[\s-]?\d{3}[\s-]?\d{4}\b|\b\d{8,15}\b/,
                message: 'Phone number detected',
                severity: 'low'
            },
            email: {
                pattern: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/,
                message: 'Email address detected',
                severity: 'low'
            },
            bitcoin: {
                pattern: /\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\b/,
                message: 'Cryptocurrency address detected',
                severity: 'high'
            },
            password: {
                pattern: /\b(?:password|passwd|pwd|pass)[\s:=]+\S+/i,
                message: 'Password detected',
                severity: 'high'
            },
            apiKey: {
                pattern: /\b(?:api[_-]?key|apikey|api[_-]?token)[\s:=]+\S+/i,
                message: 'API key detected',
                severity: 'high'
            }
        };

        // Scam keyword patterns
        this.scamKeywords = [
            /\b(?:wire|transfer|send)\s+(?:money|funds|cash|payment)/i,
            /\b(?:urgent|immediate|emergency)\s+(?:payment|transfer|help)/i,
            /\b(?:nigerian?\s+prince|inheritance|lottery\s+win)/i,
            /\b(?:click|verify)\s+(?:here|link|now)/i,
            /\b(?:suspended|locked|verify)\s+(?:account|access)/i,
            /\b(?:tax|irs|government)\s+(?:refund|payment|debt)/i,
            /\b(?:investment|profit|guaranteed)\s+(?:return|income)/i,
            /\b(?:free|winner|prize|congratulations)/i,
            /\b(?:act\s+now|limited\s+time|expires?\s+(?:today|soon))/i
        ];

        // Track warnings shown to avoid spam
        this.recentWarnings = new Map();
        this.warningCooldown = 5000; // 5 seconds between similar warnings
    }

    /**
     * Check message for sensitive information
     * @param {string} message - The message to check
     * @returns {Object} - Safety check result
     */
    checkMessage(message) {
        const detectedIssues = [];
        let highestSeverity = 'none';
        let shouldBlock = false;

        // Check for sensitive data patterns
        for (const [type, config] of Object.entries(this.sensitivePatterns)) {
            if (config.pattern.test(message)) {
                detectedIssues.push({
                    type: type,
                    message: config.message,
                    severity: config.severity
                });

                // Block high severity issues
                if (config.severity === 'high') {
                    shouldBlock = true;
                    highestSeverity = 'high';
                } else if (config.severity === 'medium' && highestSeverity !== 'high') {
                    highestSeverity = 'medium';
                } else if (config.severity === 'low' && highestSeverity === 'none') {
                    highestSeverity = 'low';
                }
            }
        }

        // Check for scam keywords
        let scamScore = 0;
        for (const pattern of this.scamKeywords) {
            if (pattern.test(message)) {
                scamScore++;
            }
        }

        if (scamScore >= 2) {
            detectedIssues.push({
                type: 'scam',
                message: 'Message contains potential scam indicators',
                severity: 'high'
            });
            shouldBlock = true;
            highestSeverity = 'high';
        } else if (scamScore === 1) {
            detectedIssues.push({
                type: 'suspicious',
                message: 'Message may be suspicious',
                severity: 'medium'
            });
            if (highestSeverity === 'none' || highestSeverity === 'low') {
                highestSeverity = 'medium';
            }
        }

        return {
            safe: detectedIssues.length === 0,
            shouldBlock: shouldBlock,
            severity: highestSeverity,
            issues: detectedIssues,
            message: message
        };
    }

    /**
     * Redact sensitive information from a message
     * @param {string} message - The message to redact
     * @returns {string} - Redacted message
     */
    redactSensitiveInfo(message) {
        let redacted = message;

        // Replace credit cards with asterisks
        redacted = redacted.replace(/\b(?:\d{4}[\s-]?){3}\d{4}\b/g, '****-****-****-****');

        // Replace SSNs
        redacted = redacted.replace(/\b\d{3}-\d{2}-\d{4}\b/g, '***-**-****');

        // Replace phone numbers (keep area code)
        redacted = redacted.replace(/\b(\d{3})[\s-]?\d{3}[\s-]?\d{4}\b/g, '$1-***-****');

        // Replace emails (keep domain)
        redacted = redacted.replace(/\b([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\.[A-Z|a-z]{2,})\b/g,
            (match, user, domain) => '*****@' + domain);

        // Replace passwords
        redacted = redacted.replace(/\b((?:password|passwd|pwd|pass)[\s:=]+)\S+/gi, '$1********');

        // Replace API keys
        redacted = redacted.replace(/\b((?:api[_-]?key|apikey|api[_-]?token)[\s:=]+)\S+/gi, '$1********');

        return redacted;
    }

    /**
     * Show warning dialog to user
     * @param {Object} checkResult - Result from checkMessage
     * @returns {Promise<boolean>} - True if user wants to send anyway
     */
    async showWarning(checkResult) {
        // Check cooldown to avoid spamming warnings
        const warningKey = checkResult.issues.map(i => i.type).join(',');
        const lastWarning = this.recentWarnings.get(warningKey);

        if (lastWarning && Date.now() - lastWarning < this.warningCooldown) {
            return false; // Don't show duplicate warnings too quickly
        }

        this.recentWarnings.set(warningKey, Date.now());

        // Create warning modal
        const modal = document.createElement('div');
        modal.className = 'safety-warning-modal';
        modal.innerHTML = `
            <div class="safety-warning-content">
                <div class="safety-warning-header">
                    <span class="safety-warning-icon">⚠️</span>
                    <h3>Security Warning</h3>
                </div>
                <div class="safety-warning-body">
                    <p class="safety-warning-main">
                        ${checkResult.shouldBlock ?
                            'This message contains sensitive information that could be used for scams.' :
                            'This message may contain sensitive information.'}
                    </p>
                    <div class="safety-warning-issues">
                        ${checkResult.issues.map(issue => `
                            <div class="safety-issue-item">
                                <span class="safety-issue-icon">${this.getSeverityIcon(issue.severity)}</span>
                                <span class="safety-issue-text">${issue.message}</span>
                            </div>
                        `).join('')}
                    </div>
                    ${checkResult.shouldBlock ? `
                        <div class="safety-warning-advice">
                            <strong>For your safety:</strong>
                            <ul>
                                <li>Never share credit card numbers in messages</li>
                                <li>Don't send passwords or personal IDs</li>
                                <li>Be cautious of requests for money or personal info</li>
                            </ul>
                        </div>
                    ` : ''}
                </div>
                <div class="safety-warning-actions">
                    <button class="safety-btn-cancel">Don't Send</button>
                    ${!checkResult.shouldBlock ?
                        '<button class="safety-btn-send">Send Anyway</button>' : ''}
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // Add animation
        setTimeout(() => modal.classList.add('show'), 10);

        return new Promise((resolve) => {
            const cancelBtn = modal.querySelector('.safety-btn-cancel');
            const sendBtn = modal.querySelector('.safety-btn-send');

            const cleanup = () => {
                modal.classList.remove('show');
                setTimeout(() => modal.remove(), 300);
            };

            cancelBtn.addEventListener('click', () => {
                cleanup();
                resolve(false);
            });

            if (sendBtn) {
                sendBtn.addEventListener('click', () => {
                    cleanup();
                    resolve(true);
                });
            }

            // Close on backdrop click
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    cleanup();
                    resolve(false);
                }
            });
        });
    }

    /**
     * Get icon for severity level
     */
    getSeverityIcon(severity) {
        switch(severity) {
            case 'high': return '🚫';
            case 'medium': return '⚠️';
            case 'low': return 'ℹ️';
            default: return '✓';
        }
    }

    /**
     * Process message before sending
     * @param {string} message - Message to send
     * @param {Function} sendCallback - Function to call if message is safe
     * @returns {Promise<boolean>} - True if message was sent
     */
    async processMessage(message, sendCallback) {
        const checkResult = this.checkMessage(message);

        if (checkResult.safe) {
            // Message is safe, send it
            await sendCallback(message);
            return true;
        }

        // Show warning
        const userChoice = await this.showWarning(checkResult);

        if (userChoice && !checkResult.shouldBlock) {
            // User chose to send anyway (only for non-blocked messages)
            console.warn('User chose to send message with warnings:', checkResult.issues);
            await sendCallback(message);
            return true;
        }

        // Message was blocked or user cancelled
        console.log('Message blocked or cancelled:', checkResult.issues);
        return false;
    }
}

// Create global instance
const messageSafety = new MessageSafety();

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = MessageSafety;
}