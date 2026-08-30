import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 10,        // 10 virtual user
  duration: '10s', // 10 second
};

export default function() {
  let res = http.get('http://localhost:8080/api/posts?page=0&size=10');
  check(res, {
    'status 200': (r) => r.status === 200,
    'response < 500ms': (r) => r.timings.duration < 500,
  });
}