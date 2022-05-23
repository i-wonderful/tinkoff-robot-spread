export default {
    name: 'Statistic',
    setup() {
        const title = 'Статистика'
        return {title}
    },
    data() {
        return {
            robotSessions: []
        }
    },
    methods: {},
    mounted() {
        console.log("mounted")
        axios.get("/statistic/all")
            .then(response => {
                this.robotSessions = response.data;
            });
    },
    template: `
        <div>
            <h5>{{title}}</h5>
            <div>
                <h5>Список сессий:</h5>
                <div v-for="rs in robotSessions" >
                    <div class="row">
                        <div class="col-3">AccountId: {{rs.accountId}}</div>
                        <div class="col-4">Время старта: <b>{{rs.startRobot}}</b></div>
                        <div class="col-5">Время завершения: <b>{{rs.endRobot}}</b></div>
                    </div>
                    <b>Завершенные заявки:</b>       
                    
                    <table class="table">
                <thead>
                    <tr>
                        <th>orderId</th>
                        <th>figi</th>
                        <th>Цена</th>
                        <th>Количество</th>
                        <th>Тип</th>
                        <th>Дата сделки</th>
                    </tr>
                </thead>
                <tbody>       
                    <tr v-for="od in rs.orderDones" >
                        <td>{{od.orderId}}</td>
                        <td>{{od.figi}}</td>
                        <td>{{od.price}}</td>
                        <td>{{od.quantity}}</td>
                        <td>{{od.direction}}</td>
                        <td>{{od.dateTimeDone}}</td>
                    </tr>
                </tbody>
            </table> 
                </div>
            </div>
        </div>
    `
}