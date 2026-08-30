/**
 * Shared Utilities for Spring Blog Frontend
 */

const getApiBase = () => {
    if (window.location.protocol === 'file:') return 'http://localhost:8080';
    const port = window.location.port;
    if (port === '8080' || port === '80' || port === '') return '';
    return 'http://localhost:8080';
};

const API_BASE = getApiBase();

/**
 * Format ISO date string into readable format (e.g. "Aug 30, 2026")
 */
function formatDate(dateString) {
    if (!dateString) return '';
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return dateString;
        return new Intl.DateTimeFormat('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        }).format(date);
    } catch (e) {
        return dateString;
    }
}

/**
 * Truncate post body to specified length
 */
function truncateText(text, limit = 150) {
    if (!text) return '';
    if (text.length <= limit) return text;
    return text.substring(0, limit) + '...';
}

function truncate(text, limit = 150) {
    return truncateText(text, limit);
}

/**
 * Extract URL Query Parameter
 */
function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

function getParam(param) {
    return getQueryParam(param);
}

