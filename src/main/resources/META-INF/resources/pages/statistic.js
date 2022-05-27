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
                var options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric' };
                this.robotSessions = response.data;
                this.robotSessions.forEach(rs => {
                    rs.startRobot = (new Date(rs.startRobot)).toLocaleDateString('RU-ru', options);
                    if (rs.endRobot != null) {
                        rs.endRobot = (new Date(rs.endRobot)).toLocaleDateString('RU-ru', options);
                    }
                    rs.orderDones.forEach(od => {
                        od.dateTimeDone = new Date(od.dateTimeDone).toLocaleTimeString('Ru-ru', {hour: 'numeric', minute: 'numeric', second: '2-digit'});
                    })
                });
            });
    },
    // <div class="col-3">AccountId: {{rs.accountId}}</div> 
    template: `
        <div>
            <h5>{{title}}</h5>
            <div>
                <h5>Список сессий:</h5>
                <div v-for="rs in robotSessions" >
                    <div class="row">
                        <div class="col-4">Время старта: <b>{{rs.startRobot}}</b></div>
                        <div class="col-5">Время завершения: <b>{{rs.endRobot}}</b></div>
                        <div class="col-3">Баланс: <b>{{rs.balance}}</b></div>
                    </div>
                    <b>Завершенные заявки:</b>       
                    
                    <table class="table">
                <thead>
                    <tr>
                        <th>orderId</th>
                        <th>figi</th>
                        <th>Цена 1 лота</th>
                        <th>Количество</th>
                        <th>Полная стоимость</th>
                        <th>Тип</th>
                        <th>Время сделки</th>
                    </tr>
                </thead>
                <tbody>       
                    <tr v-for="od in rs.orderDones" >
                        <td>{{od.orderId}}</td>
                        <td>{{od.figi}}</td>
                        <td>{{od.price}}</td>
                        <td>{{od.quantity}}</td>
                        <td>{{od.fullPrice}}</td>
                        <td>{{od.direction}}</td>
                        <td>{{od.dateTimeDone}}</td>
                    </tr>
                </tbody>
            </table> 
            <br/><br/>
                </div>
            </div>
        </div>
    `
}