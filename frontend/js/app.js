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

/**
 * Fetch and parse XML. Sitemap/RSS use XML namespaces, so callers should
 * read nodes by localName rather than CSS selectors like "url" / "item".
 */
async function fetchText(url) {
    const res = await fetch(url);
    if (!res.ok) {
        throw new Error(`Request failed (${res.status})`);
    }
    return res.text();
}

function looksLikeHtml(text) {
    const start = text.trimStart().slice(0, 200).toLowerCase();
    return start.startsWith('<!doctype html') || start.startsWith('<html');
}

async function fetchXml(url) {
    const xmlText = await fetchText(url);
    if (looksLikeHtml(xmlText)) {
        throw new Error('Got an HTML page instead of XML. Nginx is not proxying this path to the API.');
    }
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, 'application/xml');
    if (xmlDoc.getElementsByTagName('parsererror').length > 0) {
        throw new Error('Failed to parse XML response');
    }
    return xmlDoc;
}

function xmlChildren(parent, localName) {
    if (!parent) return [];
    return Array.from(parent.children).filter((node) => node.localName === localName);
}

function xmlDescendants(parent, localName) {
    if (!parent) return [];
    return Array.from(parent.getElementsByTagName('*')).filter((node) => node.localName === localName);
}

function xmlChildText(parent, localName) {
    const child = xmlChildren(parent, localName)[0];
    return child ? (child.textContent || '').trim() : '';
}

