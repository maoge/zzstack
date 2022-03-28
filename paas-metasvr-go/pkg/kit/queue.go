package kit

import "sync"

// https://github.com/bigwhite/experiments/tree/master/emptyinterface2any/any.go

// 通过条件编译来支持g1.18以下版本
// // +build !go1.18
// //go:build !go1.18
// type any = interface{}

type queue struct {
	mut  sync.Mutex
	nums []any
}

func NewQueue() *queue {
	return &queue{nums: make([]any, 0)}
}

func (q *queue) Push(t any) {
	q.mut.Lock()
	defer q.mut.Unlock()

	q.nums = append(q.nums, t)
}

func (q *queue) Pop() any {
	q.mut.Lock()
	defer q.mut.Unlock()

	res := q.nums[0]
	q.nums = q.nums[1:]
	return res
}

func (q *queue) Len() int {
	q.mut.Lock()
	defer q.mut.Unlock()

	return len(q.nums)
}

func (q *queue) IsEmpty() bool {
	return q.Len() == 0
}
